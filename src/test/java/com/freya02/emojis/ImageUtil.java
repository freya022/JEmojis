package com.freya02.emojis;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Path;

public class ImageUtil {
	//extra slow no native helpers D:
	public static void saveImage(Image image, Path imagePath) throws IOException {
		if (!ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", imagePath.toFile())) {
			throw new IOException("Couldn't save image");
		}
	}
}
