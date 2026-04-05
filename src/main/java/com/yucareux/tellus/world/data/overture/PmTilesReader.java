package com.yucareux.tellus.world.data.overture;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.yucareux.tellus.Tellus;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import org.jspecify.annotations.Nullable;

final class PmTilesReader {
	private static final int HEADER_SIZE = 127;
	private static final int MAX_DIRECTORY_DEPTH = 4;
	private static final int MAX_DIRECTORY_CACHE = intProperty("tellus.overture.dirCache", 256);
	private static final int READ_TIMEOUT_MS = 15000;
	private static final int CONNECT_TIMEOUT_MS = 10000;

	private final String url;
	private final LoadingCache<DirectoryKey, Directory> directoryCache;
	private @Nullable PmTilesHeader header;
	private @Nullable Directory rootDirectory;
	private int directoryCompression = 2;

	PmTilesReader(final String url) {
		this.url = Objects.requireNonNull(url, "url");
		this.directoryCache = CacheBuilder.newBuilder()
				.maximumSize(MAX_DIRECTORY_CACHE)
				.build(new CacheLoader<>() {
					@Override
					public Directory load(final DirectoryKey key) throws Exception {
						return readDirectory(key.offset, key.length);
					}
				});
	}

	PmTilesHeader header() throws IOException {
		if (this.header == null) {
			this.header = readHeader();
		}
		return this.header;
	}

	byte @Nullable [] getTileBytes(final int z, final int x, final int y) throws IOException {
		long tileId = zxyToTileId(z, x, y);
		PmTilesHeader header = header();
		Directory directory = getRootDirectory();

		for (int depth = 0; depth < MAX_DIRECTORY_DEPTH; depth++) {
			Entry entry = findTile(directory.entries, tileId);
			if (entry == null) {
				return null;
			}
			if (entry.runLength == 0) {
				long dirOffset = header.leafDirectoryOffset + entry.offset;
				long dirLength = entry.length;
				directory = getDirectory(dirOffset, dirLength);
				continue;
			}
			long dataOffset = header.tileDataOffset + entry.offset;
			if (entry.length > Integer.MAX_VALUE) {
				throw new IOException("Tile too large");
			}
			return readBytes(dataOffset, (int) entry.length);
		}
		return null;
	}

	private Directory getRootDirectory() throws IOException {
		if (this.rootDirectory == null) {
			PmTilesHeader header = header();
			this.rootDirectory = getDirectory(header.rootOffset, header.rootLength);
		}
		return this.rootDirectory;
	}

	private Directory getDirectory(final long offset, final long length) throws IOException {
		try {
			return this.directoryCache.get(new DirectoryKey(offset, length));
		} catch (Exception e) {
			if (e.getCause() instanceof IOException io) {
				throw io;
			}
			throw new IOException("Failed to read PMTiles directory", e);
		}
	}

	private PmTilesHeader readHeader() throws IOException {
		byte[] headerBytes = readBytes(0, HEADER_SIZE);
		if (!"PMTiles".equals(new String(headerBytes, 0, 7, StandardCharsets.US_ASCII))) {
			throw new IOException("PMTiles header missing");
		}
		int version = headerBytes[7] & 0xFF;
		if (version != 3) {
			throw new IOException("Unsupported PMTiles version " + version);
		}
		long rootOffset = readUint64(headerBytes, 8);
		long rootLength = readUint64(headerBytes, 16);
		long metadataOffset = readUint64(headerBytes, 24);
		long metadataLength = readUint64(headerBytes, 32);
		long leafOffset = readUint64(headerBytes, 40);
		long leafLength = readUint64(headerBytes, 48);
		long tileOffset = readUint64(headerBytes, 56);
		long tileLength = readUint64(headerBytes, 64);
		int internalCompression = headerBytes[97] & 0xFF;
		int tileCompression = headerBytes[98] & 0xFF;
		int tileType = headerBytes[99] & 0xFF;
		int minZoom = headerBytes[100] & 0xFF;
		int maxZoom = headerBytes[101] & 0xFF;
		this.directoryCompression = internalCompression;

		if (internalCompression != 1 && internalCompression != 2) {
			Tellus.LOGGER.warn("Unexpected PMTiles directory compression {} for {}", internalCompression, this.url);
		}

		return new PmTilesHeader(
				rootOffset,
				rootLength,
				metadataOffset,
				metadataLength,
				leafOffset,
				leafLength,
				tileOffset,
				tileLength,
				minZoom,
				maxZoom,
				internalCompression,
				tileCompression,
				tileType);
	}

	private Directory readDirectory(final long offset, final long length) throws IOException {
		if (length <= 0) {
			return new Directory(List.of());
		}
		byte[] encoded = readBytes(offset, (int) length);
		final byte[] decompressed;
		if (this.directoryCompression == 2) {
			decompressed = gunzip(encoded);
		} else if (this.directoryCompression == 1) {
			decompressed = encoded;
		} else {
			throw new IOException("Unsupported PMTiles directory compression " + this.directoryCompression);
		}
		ByteArrayInputStream input = new ByteArrayInputStream(decompressed);
		int numEntries = (int) readVarint(input);
		List<Entry> entries = new ArrayList<>(numEntries);
		long lastId = 0;
		for (int i = 0; i < numEntries; i++) {
			long delta = readVarint(input);
			long tileId = lastId + delta;
			entries.add(new Entry(tileId, 0, 0, 0));
			lastId = tileId;
		}
		for (int i = 0; i < numEntries; i++) {
			entries.get(i).runLength = readVarint(input);
		}
		for (int i = 0; i < numEntries; i++) {
			entries.get(i).length = readVarint(input);
		}
		for (int i = 0; i < numEntries; i++) {
			long tmp = readVarint(input);
			if (i > 0 && tmp == 0) {
				Entry prev = entries.get(i - 1);
				entries.get(i).offset = prev.offset + prev.length;
			} else {
				entries.get(i).offset = tmp - 1;
			}
		}
		return new Directory(entries);
	}

	private byte[] readBytes(final long offset, final int length) throws IOException {
		if (length <= 0) {
			return new byte[0];
		}
		HttpURLConnection connection = (HttpURLConnection) URI.create(this.url).toURL().openConnection();
		connection.setRequestProperty("Range", "bytes=" + offset + "-" + (offset + length - 1));
		connection.setInstanceFollowRedirects(true);
		connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
		connection.setReadTimeout(READ_TIMEOUT_MS);
		int code = connection.getResponseCode();
		try (InputStream input = connection.getInputStream()) {
			if (code == HttpURLConnection.HTTP_OK) {
				skipFully(input, offset);
				return readFully(input, length);
			}
			if (code != HttpURLConnection.HTTP_PARTIAL) {
				throw new IOException("PMTiles HTTP error " + code + " for " + this.url);
			}
			return readFully(input, length);
		} finally {
			connection.disconnect();
		}
	}

	private static byte[] gunzip(final byte[] input) throws IOException {
		try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(input));
				 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[8192];
			int read;
			while ((read = gzip.read(buffer)) != -1) {
				output.write(buffer, 0, read);
			}
			return output.toByteArray();
		}
	}

	private static void skipFully(final InputStream input, final long bytes) throws IOException {
		long remaining = bytes;
		while (remaining > 0) {
			long skipped = input.skip(remaining);
			if (skipped <= 0) {
				if (input.read() == -1) {
					throw new EOFException("Unexpected EOF while skipping");
				}
				remaining--;
			} else {
				remaining -= skipped;
			}
		}
	}

	private static byte[] readFully(final InputStream input, final int length) throws IOException {
		byte[] buffer = new byte[length];
		int offset = 0;
		while (offset < length) {
			int read = input.read(buffer, offset, length - offset);
			if (read == -1) {
				throw new EOFException("Unexpected EOF while reading");
			}
			offset += read;
		}
		return buffer;
	}

	private static long readVarint(final InputStream input) throws IOException {
		long result = 0;
		int shift = 0;
		while (true) {
			int raw = input.read();
			if (raw == -1) {
				throw new EOFException("Unexpected EOF in varint");
			}
			result |= (long) (raw & 0x7F) << shift;
			if ((raw & 0x80) == 0) {
				break;
			}
			shift += 7;
		}
		return result;
	}

	private static long readUint64(final byte[] buffer, final int pos) {
		return ((long) buffer[pos] & 0xFF)
				| (((long) buffer[pos + 1] & 0xFF) << 8)
				| (((long) buffer[pos + 2] & 0xFF) << 16)
				| (((long) buffer[pos + 3] & 0xFF) << 24)
				| (((long) buffer[pos + 4] & 0xFF) << 32)
				| (((long) buffer[pos + 5] & 0xFF) << 40)
				| (((long) buffer[pos + 6] & 0xFF) << 48)
				| (((long) buffer[pos + 7] & 0xFF) << 56);
	}

	private static long zxyToTileId(int z, int x, int y) {
		if (z > 31) {
			throw new IllegalArgumentException("Tile zoom exceeds 64-bit limit");
		}
		int max = (1 << z) - 1;
		if (x < 0 || y < 0 || x > max || y > max) {
			throw new IllegalArgumentException("Tile x/y outside zoom bounds");
		}
		long acc = ((1L << (z * 2)) - 1) / 3;
		long hilbert = 0L;
		for (int a = z - 1; a >= 0; a--) {
			int s = 1 << a;
			int rx = (x & s) != 0 ? 1 : 0;
			int ry = (y & s) != 0 ? 1 : 0;
			hilbert += ((long) ((3 * rx) ^ ry)) << (a * 2);
			int[] rotated = rotate(s, x, y, rx, ry);
			x = rotated[0];
			y = rotated[1];
		}
		return acc + hilbert;
	}

	private static int[] rotate(final int n, int x, int y, final int rx, final int ry) {
		if (ry == 0) {
			if (rx != 0) {
				x = n - 1 - x;
				y = n - 1 - y;
			}
			int t = x;
			x = y;
			y = t;
		}
		return new int[] { x, y };
	}

	private static Entry findTile(final List<Entry> entries, final long tileId) {
		int m = 0;
		int n = entries.size() - 1;
		while (m <= n) {
			int k = (n + m) >> 1;
			long diff = tileId - entries.get(k).tileId;
			if (diff > 0) {
				m = k + 1;
			} else if (diff < 0) {
				n = k - 1;
			} else {
				return entries.get(k);
			}
		}
		if (n >= 0) {
			Entry entry = entries.get(n);
			if (entry.runLength == 0) {
				return entry;
			}
			if (tileId - entry.tileId < entry.runLength) {
				return entry;
			}
		}
		return null;
	}

	private static int intProperty(final String key, final int defaultValue) {
		String value = System.getProperty(key);
		if (value == null) {
			return defaultValue;
		}
		try {
			return Math.max(1, Integer.parseInt(value));
		} catch (NumberFormatException ignored) {
			return defaultValue;
		}
	}

	private record DirectoryKey(long offset, long length) {
	}

	private static final class Entry {
		private final long tileId;
		private long offset;
		private long length;
		private long runLength;

		private Entry(final long tileId, final long offset, final long length, final long runLength) {
			this.tileId = tileId;
			this.offset = offset;
			this.length = length;
			this.runLength = runLength;
		}
	}

	private static final class Directory {
		private final List<Entry> entries;

		private Directory(final List<Entry> entries) {
			this.entries = entries;
		}
	}

	static final class PmTilesHeader {
		private final long rootOffset;
		private final long rootLength;
		private final long metadataOffset;
		private final long metadataLength;
		private final long leafDirectoryOffset;
		private final long leafDirectoryLength;
		private final long tileDataOffset;
		private final long tileDataLength;
		private final int minZoom;
		private final int maxZoom;
		private final int internalCompression;
		private final int tileCompression;
		private final int tileType;

		private PmTilesHeader(
				final long rootOffset,
				final long rootLength,
				final long metadataOffset,
				final long metadataLength,
				final long leafDirectoryOffset,
				final long leafDirectoryLength,
				final long tileDataOffset,
				final long tileDataLength,
				final int minZoom,
				final int maxZoom,
				final int internalCompression,
				final int tileCompression,
				final int tileType) {
			this.rootOffset = rootOffset;
			this.rootLength = rootLength;
			this.metadataOffset = metadataOffset;
			this.metadataLength = metadataLength;
			this.leafDirectoryOffset = leafDirectoryOffset;
			this.leafDirectoryLength = leafDirectoryLength;
			this.tileDataOffset = tileDataOffset;
			this.tileDataLength = tileDataLength;
			this.minZoom = minZoom;
			this.maxZoom = maxZoom;
			this.internalCompression = internalCompression;
			this.tileCompression = tileCompression;
			this.tileType = tileType;
		}

		public long rootOffset() {
			return this.rootOffset;
		}

		public long rootLength() {
			return this.rootLength;
		}

		public long metadataOffset() {
			return this.metadataOffset;
		}

		public long metadataLength() {
			return this.metadataLength;
		}

		public long leafDirectoryOffset() {
			return this.leafDirectoryOffset;
		}

		public long leafDirectoryLength() {
			return this.leafDirectoryLength;
		}

		public long tileDataOffset() {
			return this.tileDataOffset;
		}

		public long tileDataLength() {
			return this.tileDataLength;
		}

		public int minZoom() {
			return this.minZoom;
		}

		public int maxZoom() {
			return this.maxZoom;
		}

		public int internalCompression() {
			return this.internalCompression;
		}

		public int tileCompression() {
			return this.tileCompression;
		}

		public int tileType() {
			return this.tileType;
		}
	}
}
