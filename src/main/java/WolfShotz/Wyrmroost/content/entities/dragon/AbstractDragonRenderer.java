package WolfShotz.Wyrmroost.content.entities.dragon;

import WolfShotz.Wyrmroost.util.utils.ModUtils;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.lwjgl.opengl.GL11.GL_ONE;

public abstract class AbstractDragonRenderer<T extends AbstractDragonEntity> extends MobRenderer<T, EntityModel<T>>
{
    public static final String DEF_LOC = "textures/entity/dragon/";
    public boolean isChristmas = false;

    public AbstractDragonRenderer(EntityRendererManager manager, EntityModel<T> model, float shadowSize) {
        super(manager, model, shadowSize);
    
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        if (calendar.get(Calendar.MONTH) == Calendar.DECEMBER && (day > 14 && day < 26)) isChristmas = true;
    }
    
    public abstract String getResourceDirectory();
    
    public ResourceLocation location(String png) { return ModUtils.location(getResourceDirectory() + png); }
    
    // =================
    //   Render Layers
    // =================
    
    /**
     * Abstract layer renderer to handle the generalizing
     */
    public abstract class AbstractLayerRenderer extends LayerRenderer<T, EntityModel<T>>
    {
        public AbstractLayerRenderer(IEntityRenderer<T, EntityModel<T>> entityIn) { super(entityIn); }
        
        @Override // Override to deobfuscate params
        public abstract void render(T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale);
        
        @Override
        public boolean shouldCombineTextures() { return false; }
    }
    
    /**
     * Class Responsible for glow layer of dragons
     * Used mainly for eyes, but can be used for other things aswell.
     */
    public class GlowLayer extends AbstractLayerRenderer
    {
        private Function<T, ResourceLocation> glowLocation;
    
        public GlowLayer(IEntityRenderer<T, EntityModel<T>> entityIn, Function<T, ResourceLocation> glowLocation) {
            super(entityIn);
            this.glowLocation = glowLocation;
        }
    
        @Override
        public void render(T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
            GameRenderer gamerenderer = Minecraft.getInstance().gameRenderer;
            int i = entity.getBrightnessForRender();
            int j = i % 65536;
            int k = i / 65536;
    
            bindTexture(glowLocation.apply(entity));
    
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL_ONE, GL_ONE);
            GlStateManager.depthMask(!entity.isInvisible());
            GLX.glMultiTexCoord2f(GLX.GL_TEXTURE1, 240, 240);
            GlStateManager.color4f(1f, 1f, 1f, 1f);
    
            gamerenderer.setupFogColor(true);
            getEntityModel().render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            gamerenderer.setupFogColor(false);
    
            GLX.glMultiTexCoord2f(GLX.GL_TEXTURE1, (float) j, (float) k);
            // setLightMap(Entity)
            func_215334_a(entity);
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
        }
    }
    
    /**
     * A render layer that can only render if certain conditions are met.
     * E.G. is the dragon sleeping, saddled, etc
     */
    public class ConditionalLayer extends AbstractLayerRenderer
    {
        private ResourceLocation loc;
        private Predicate<T> conditions;
        private Function<T, ResourceLocation> func;
        
        public ConditionalLayer(IEntityRenderer<T, EntityModel<T>> entityIn, ResourceLocation locIn, Predicate<T> conditions) {
            super(entityIn);
            this.loc = locIn;
            this.conditions = conditions;
        }
    
        public ConditionalLayer(IEntityRenderer<T, EntityModel<T>> entityIn, Function<T, ResourceLocation> funcIn, Predicate<T> conditions) {
            super(entityIn);
            this.func = funcIn;
            this.conditions = conditions;
        }
    
        @Override
        public void render(T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
            if (!conditions.test(entity)) return;
            if (func != null) loc = func.apply(entity);
            
            bindTexture(loc);
            getEntityModel().render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }
    }
    
    /**
     * Class Responsible for the sleep layer. normally consists of closed eyes
     */
    public class SleepLayer extends ConditionalLayer
    {
        public SleepLayer(IEntityRenderer<T, EntityModel<T>> entityIn, ResourceLocation locIn) {
            super(entityIn, locIn, AbstractDragonEntity::isSleeping);
        }
    }
}
