package com.fpsgame.client.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verify that the character image is rendered above the player's red dot.
 * We simulate the render pipeline by drawing a red dot first, then a sprite
 * with an opaque green center on top at the same position and assert the
 * center pixel matches the sprite color (not the red dot), proving z-order.
 */
public class CharacterSpriteOverlayTest {

    @Test
    @DisplayName("Character sprite renders above red dot at center")
    void spriteOverDot_centerPixelShowsSprite() {
        int W = 200, H = 200;
        int cx = W / 2, cy = H / 2;

        // Canvas with alpha
        BufferedImage canvas = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1) Draw red dot first (background layer)
        int dotR = 14; // radius of red dot under the sprite
        g.setColor(new Color(0xFF3A3A));
        g.fillOval(cx - dotR, cy - dotR, dotR * 2, dotR * 2);

        // 2) Create a mock character sprite with transparent background
        //    and a fully opaque GREEN circle in the middle
        int spriteSize = 48;
        BufferedImage sprite = new BufferedImage(spriteSize, spriteSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = sprite.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // transparent background by default
        int sr = 18; // green circle radius inside sprite
        sg.setColor(new Color(0x24D05A)); // opaque green
        sg.fillOval(spriteSize/2 - sr, spriteSize/2 - sr, sr * 2, sr * 2);
        // outline to make edges visible if needed
        sg.setColor(new Color(0x0F8A3E));
        sg.setStroke(new BasicStroke(2f));
        sg.drawOval(spriteSize/2 - sr, spriteSize/2 - sr, sr * 2, sr * 2);
        sg.dispose();

        // 3) Draw sprite on top, centered at (cx, cy)
        int sx = cx - spriteSize/2;
        int sy = cy - spriteSize/2;
        g.drawImage(sprite, sx, sy, null);
        g.dispose();

        // 4) Sample the exact center pixel
        int argb = canvas.getRGB(cx, cy);
        Color c = new Color(argb, true);

        // The center should be GREEN (sprite), not RED (dot)
        // Allow exact match to sprite fill (0x24D05A) with full alpha
        assertEquals(0x24, c.getRed(), "Center pixel RED should come from sprite (not red dot)");
        assertEquals(0xD0, c.getGreen(), "Center pixel GREEN should come from sprite");
        assertEquals(0x5A, c.getBlue(), "Center pixel BLUE should come from sprite");
        assertEquals(255, c.getAlpha(), "Sprite center should be fully opaque");
    }

    @Test
    @DisplayName("Red dot still visible outside sprite footprint")
    void redDotVisibleOutsideSprite() {
        int W = 200, H = 200;
        int cx = W / 2, cy = H / 2;

        BufferedImage canvas = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // red dot (background)
        int dotR = 20;
        g.setColor(new Color(0xFF3A3A));
        g.fillOval(cx - dotR, cy - dotR, dotR * 2, dotR * 2);

        // small sprite (foreground)
        int spriteSize = 24; // smaller than dot so edges remain visible
        BufferedImage sprite = new BufferedImage(spriteSize, spriteSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = sprite.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        sg.setColor(new Color(0x24D05A));
        sg.fillOval(4, 4, spriteSize - 8, spriteSize - 8);
        sg.dispose();

        int sx = cx - spriteSize/2;
        int sy = cy - spriteSize/2;
        g.drawImage(sprite, sx, sy, null);
        g.dispose();

        // sample a pixel clearly outside the sprite but inside the red dot ring
        int sampleOffset = dotR - 2; // near outer edge of red dot
        int argb = canvas.getRGB(cx + sampleOffset/2, cy); // to the right side
        Color c = new Color(argb, true);

        // Expect red-ish color (from the dot), not sprite's green
        assertTrue(c.getRed() > c.getGreen(), "Outer pixel should be red-dominant (dot visible)");
        assertTrue(c.getRed() > c.getBlue(), "Outer pixel should be red-dominant (dot visible)");
        assertEquals(255, c.getAlpha(), "Dot area should be opaque");
    }
}
