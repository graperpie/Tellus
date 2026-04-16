## NOTE: very outdated compared to my local branch. i'll update this at some point unless someone really wants to look into the code.

# a tellus fork that modifies the LOD generation code to stream in satelite imagery at various zoom(resolution) levels to compensate for N-sizd LOD tiles/sections.

## The Problem
currently, Tellus loads land cover, which is arguably accurate and an ideal type of data to use to determine what areas are sand, snow, grass, as well as water and trees. the problem with this, is that there is only one available resolution (1:10) for it. this means that for every 10 blocks or so, the client has to download new land cover data. 

albeit accurate, this is extremely inefficient. Distant Horizons having to locally downsample the data it gets means that the maximum generation speed is limited to the user's bandwidth, and the further you generate, the slower the generation feels. 

this satelite imagery implementation is made to counter that. because it is available at different zoom levels, we are able to assign what zoom levels are streamed in at any given detail level (lower zoom levels, longer distance). this means that the gen speed actually gradually increases

rough graph showcase:
<img width="1312" height="937" alt="chart@600dpi" src="https://github.com/user-attachments/assets/a338f814-c03f-4f2c-a79b-7e8e94e06c08" />
