/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1044
 *  net.minecraft.class_1049
 *  net.minecraft.class_2960
 *  net.minecraft.class_3300
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.iris.pbr.loader;

import java.io.IOException;
import net.irisshaders.iris.mixin.texture.SimpleTextureAccessor;
import net.irisshaders.iris.pbr.loader.PBRTextureLoader;
import net.irisshaders.iris.pbr.texture.PBRType;
import net.minecraft.class_1044;
import net.minecraft.class_1049;
import net.minecraft.class_2960;
import net.minecraft.class_3300;
import org.jetbrains.annotations.Nullable;

public class SimplePBRLoader
implements PBRTextureLoader<class_1049> {
    @Override
    public void load(class_1049 texture, class_3300 resourceManager, PBRTextureLoader.PBRTextureConsumer pbrTextureConsumer) {
        class_2960 location = ((SimpleTextureAccessor)texture).getLocation();
        class_1044 normalTexture = this.createPBRTexture(location, resourceManager, PBRType.NORMAL);
        class_1044 specularTexture = this.createPBRTexture(location, resourceManager, PBRType.SPECULAR);
        if (normalTexture != null) {
            pbrTextureConsumer.acceptNormalTexture(normalTexture);
        }
        if (specularTexture != null) {
            pbrTextureConsumer.acceptSpecularTexture(specularTexture);
        }
    }

    @Nullable
    protected class_1044 createPBRTexture(class_2960 imageLocation, class_3300 resourceManager, PBRType pbrType) {
        class_2960 pbrImageLocation = imageLocation.method_45134(pbrType::appendSuffix);
        class_1049 pbrTexture = new class_1049(pbrImageLocation);
        try {
            pbrTexture.method_4625(resourceManager);
        }
        catch (IOException e) {
            return null;
        }
        return pbrTexture;
    }
}

