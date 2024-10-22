package me.x150.renderer.font;

import me.x150.renderer.util.RendererUtils;
import net.minecraft.client.texture.NativeImageBackedTexture;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GlyphMap {
	final char fromIncl, toExcl;
	final Font font;
	final int pixelPadding;
	private final Glyph[] glyphs;
	NativeImageBackedTexture texture;
	int width, height;

	boolean generated = false;

	public GlyphMap(char fromIncl, char toExcl, Font font, int pixelPadding) {
		this.fromIncl = fromIncl;
		this.toExcl = toExcl;
		this.font = font;
		this.pixelPadding = pixelPadding;
		this.glyphs = new Glyph[toExcl - fromIncl];
	}

	public Glyph getGlyph(char c) {
		synchronized (this) {
			if (!generated) {
				privateGenerate();
			}
			return glyphs[c - fromIncl];
		}
	}

	public void destroy() {
		synchronized (this) {
			generated = false;
			if (texture != null) texture.close();
			Arrays.fill(glyphs, null);
			this.width = -1;
			this.height = -1;
		}
	}

	public boolean contains(char c) {
		return c >= fromIncl && c < toExcl;
	}

	public void generate() {
		synchronized (this) {
			privateGenerate();
		}
	}

	private void privateGenerate() {
		if (generated) {
			return;
		}
		int range = toExcl - fromIncl - 1;
		int charsVert = (int) (Math.ceil(Math.sqrt(range)) * 1.5);  // double as many chars wide as high
		int generatedChars = 0;
		int charNX = 0;
		int maxX = 0, maxY = 0;
		int currentX = 0, currentY = 0;
		int currentRowMaxY = 0;
		List<Glyph> glyphs1 = new ArrayList<>();
		AffineTransform af = new AffineTransform();
		FontRenderContext frc = new FontRenderContext(af, true, false);
		while (generatedChars <= range) {
			char currentChar = (char) (fromIncl + generatedChars);
			Rectangle2D stringBounds = this.font.getStringBounds(String.valueOf(currentChar), frc);

			int width = (int) Math.ceil(stringBounds.getWidth());
			int height = (int) Math.ceil(stringBounds.getHeight());
			generatedChars++;
			maxX = Math.max(maxX, currentX + width);
			maxY = Math.max(maxY, currentY + height);
			if (charNX >= charsVert) {
				currentX = 0;
				currentY += currentRowMaxY + pixelPadding; // add height of highest glyph, and reset
				charNX = 0;
				currentRowMaxY = 0;
			}
			currentRowMaxY = Math.max(currentRowMaxY, height); // calculate the highest glyph in this row
			Glyph gl = new Glyph(currentX, currentY, width, height,
					currentX, currentY, 0, 0, width, height, currentChar, this);
			glyphs1.add(gl);
			currentX += width + pixelPadding;
			charNX++;
		}
		BufferedImage bi = new BufferedImage(Math.max(maxX + pixelPadding, 1), Math.max(maxY + pixelPadding, 1),
				BufferedImage.TYPE_INT_ARGB);
		width = bi.getWidth();
		height = bi.getHeight();
		Graphics2D g2d = bi.createGraphics();
		g2d.setColor(new Color(255, 255, 255, 0));
		g2d.fillRect(0, 0, width, height);
		g2d.setColor(Color.WHITE);

		g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		g2d.setFont(font);
		FontMetrics fontMetrics = g2d.getFontMetrics();
		for (Glyph glyph : glyphs1) {
			g2d.drawString(String.valueOf(glyph.value()), (int) glyph.tlU(), (int) (glyph.tlV() + fontMetrics.getAscent()));
			glyphs[glyph.value() - fromIncl] = glyph;
		}
//		try {
//			ImageIO.write(bi, "png", Path.of("dump").resolve("page%d-%d%d.png".formatted((int) fromIncl, (int) toExcl, font.getStyle())).toFile());
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
		this.texture = RendererUtils.bufferedImageToNIBT(bi);
		generated = true;
	}
}
