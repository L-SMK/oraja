package bms.player.beatoraja.play.bga;

import bms.model.TimeLine;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.play.SkinBGA;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Rectangle;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BgaDisplayToggleTest {
    private static final class TestTexture extends Texture {
        private TestTexture() { super((Pixmap) null); }
        @Override public int getWidth() { return 2; }
        @Override public int getHeight() { return 2; }
        @Override public void setFilter(TextureFilter min, TextureFilter mag) { }
    }
    private static final class TestBatch extends SpriteBatch {
        int imageDraws;
        @Override public void draw(Texture texture, float x, float y, float w, float h) { }
        @Override public void draw(TextureRegion region, float x, float y, float w, float h) { imageDraws++; }
        @Override public void setColor(Color color) { }
        @Override public Color getColor() { return Color.WHITE; }
        @Override public void setShader(ShaderProgram shader) { }
        @Override public void setBlendFunction(int source, int dest) { }
    }
    private static final class TestSkin extends SkinBGA {
        TestSkin() { super(Config.BGAEXPAND_FULL); }
        @Override public Color getColor() { return Color.WHITE; }
    }
    private static final class Movie implements MovieProcessor {
        final Texture frame;
        int starts, frames;
        long lastTime;
        Movie(Texture texture) { frame = texture; }
        @Override public Texture getFrame(long time) { frames++; lastTime = time; return frame; }
        @Override public void play(long time, boolean loop) { starts++; }
        @Override public void stop() { }
        @Override public void dispose() { }
    }
    private static void set(Object object, Class<?> type, String key, Object value) throws Exception {
        Field field = type.getDeclaredField(key); field.setAccessible(true); field.set(object, value);
    }
    private static sun.misc.Unsafe unsafe() throws Exception {
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        return (sun.misc.Unsafe) field.get(null);
    }

    @Test void hiddenBgaKeepsMoviesAndLayersAtChartTimeWithoutRestartOnShow() throws Exception {
        // Allocate only test graphics objects: no GL context or application window.
        Texture texture = (Texture) unsafe().allocateInstance(TestTexture.class);
        TestBatch batch = (TestBatch) unsafe().allocateInstance(TestBatch.class);
        SkinObjectRenderer sprite = (SkinObjectRenderer) unsafe().allocateInstance(SkinObjectRenderer.class);
        set(sprite, SkinObjectRenderer.class, "sprite", batch);
        set(sprite, SkinObjectRenderer.class, "shaders", new ShaderProgram[6]);
        set(sprite, SkinObjectRenderer.class, "color", Color.WHITE.cpy());
        Movie main = new Movie(texture), layer = new Movie(texture), next = new Movie(texture);
        TimeLine first = new TimeLine(0, 0, 0); first.setBGA(0); first.setLayer(1);
        TimeLine second = new TimeLine(1, 500_000, 0); second.setBGA(2);
        PlayerConfig player = new PlayerConfig(); player.setBmsirBgaDisplayEnabled(false);
        BGAProcessor processor = (BGAProcessor) unsafe().allocateInstance(BGAProcessor.class);
        set(processor, BGAProcessor.class, "player", player);
        set(processor, BGAProcessor.class, "movies", new MovieProcessor[]{main, layer, next});
        set(processor, BGAProcessor.class, "timelines", new TimeLine[]{first, second});
        set(processor, BGAProcessor.class, "progress", 1f); set(processor, BGAProcessor.class, "time", -1L);
        set(processor, BGAProcessor.class, "playingbgaid", -1); set(processor, BGAProcessor.class, "playinglayerid", -1);
        set(processor, BGAProcessor.class, "blanktex", texture);
        set(processor, BGAProcessor.class, "image", new TextureRegion()); set(processor, BGAProcessor.class, "tmpRect", new Rectangle());
        SkinBGA skin = new TestSkin(); Rectangle rect = new Rectangle(0, 0, 100, 100);
        processor.prepareBGA(250); processor.drawBGA(skin, sprite, rect);
        processor.prepareBGA(750); processor.drawBGA(skin, sprite, rect);
        assertEquals(0, batch.imageDraws);
        assertEquals(1, main.starts); assertEquals(1, next.starts); assertEquals(1, layer.starts);
        assertEquals(750, next.lastTime); assertEquals(750, layer.lastTime);
        player.setBmsirBgaDisplayEnabled(true);
        processor.prepareBGA(1000); processor.drawBGA(skin, sprite, rect);
        assertEquals(2, batch.imageDraws); assertEquals(1, next.starts); assertEquals(1, layer.starts);
        assertEquals(1000, next.lastTime); assertEquals(1000, layer.lastTime);
    }
}
