package net.como.client.utils;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import net.como.client.ComoClient;
import net.como.client.structures.Colour;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec2f;

public class Render2DUtils {
    public static class BufferContainer {
        Matrix4f matrix;
        BufferBuilder bufferBuilder;

        private void open(MatrixStack matrixStack, VertexFormat.DrawMode drawMode, VertexFormat format) {
            this.matrix = matrixStack.peek().getPositionMatrix();
            this.bufferBuilder = Tessellator.getInstance().getBuffer();
            RenderSystem.setShader(GameRenderer::getPositionShader);

            this.bufferBuilder.begin(drawMode, format);
        }

        public BufferContainer(MatrixStack matrixStack, VertexFormat.DrawMode drawMode, VertexFormat format) {
            this.open(matrixStack, drawMode, format);
        }

        public BufferContainer(MatrixStack matrixStack) {
            this.open(matrixStack, VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION);
        }

        public void close() {
            this.bufferBuilder.end();
            BufferRenderer.draw(bufferBuilder);            
        }

        public void vertex2D(int x, int y) {
            this.bufferBuilder.vertex(x, y, 0).next();
        }
    }

    public static Vec2f relPosition(Vec2f vec) {
        return relPosition((int)vec.x, (int)vec.y);
    }

    public static Vec2f relPosition(int x, int y) {
        return new Vec2f(
            (float)x,
            (float)y
        );
    }
    
    public static void begin(MatrixStack matrixStack, Colour colour) {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);

		matrixStack.push();
        RenderSystem.setShaderColor(
            RenderUtils.normaliseColourPart(colour.r),
            RenderUtils.normaliseColourPart(colour.g),
            RenderUtils.normaliseColourPart(colour.b),
            RenderUtils.normaliseColourPart(colour.a)
        );
    }

    public static void finish(MatrixStack matrixStack) {
		matrixStack.pop();

		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }

    public static void renderLine(MatrixStack matrixStack, int x, int y, int x2, int y2, Colour colour) {
        begin(matrixStack, colour);
        BufferContainer bufferContainer = new BufferContainer(matrixStack);

        bufferContainer.vertex2D(x, y);
        bufferContainer.vertex2D(x2, y2);

        bufferContainer.close();
        finish(matrixStack);
    }

    public static void renderLine(MatrixStack matrixStack, int x, int y, int x2, int y2) {
        renderLine(matrixStack, x, y, x2, y2, new Colour(255, 255, 255, 255));
    }

    public static void renderBox(MatrixStack matrixStack, int x, int y, int x2, int y2, Colour colour) {
        begin(matrixStack, colour);
        BufferContainer bufferContainer = new BufferContainer(matrixStack);

        // Top
        bufferContainer.vertex2D(x, y);
        bufferContainer.vertex2D(x2, y);

        // Right
        bufferContainer.vertex2D(x2, y);
        bufferContainer.vertex2D(x2, y2);

        // Left
        bufferContainer.vertex2D(x, y);
        bufferContainer.vertex2D(x, y2);

        // Bottom
        bufferContainer.vertex2D(x, y2);
        bufferContainer.vertex2D(x2, y2);

        bufferContainer.close();
        finish(matrixStack);
    }

    public static void renderBox(MatrixStack matrixStack, int x, int y, int x2, int y2) {
        renderBox(matrixStack, x, y, x2, y2, new Colour(255, 255, 255, 255));
    }

    public static void renderBackground(MatrixStack matrixStack, int x, int y, int x2, int y2, Colour colour) {
        begin(matrixStack, colour);
        BufferContainer bufferContainer = new BufferContainer(matrixStack, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

        bufferContainer.vertex2D(x, y);
        bufferContainer.vertex2D(x, y2);
        bufferContainer.vertex2D(x2, y2);
        bufferContainer.vertex2D(x2, y);

        bufferContainer.close();
        finish(matrixStack);
    }

    public static void renderBackgroundBox(MatrixStack matrixStack, int x, int y, int x2, int y2, Colour bgColour, Colour outline) {
        renderBackground(matrixStack, x, y, x2, y2, bgColour);
        renderBox(matrixStack, x, y, x2, y2, outline);
    }
}