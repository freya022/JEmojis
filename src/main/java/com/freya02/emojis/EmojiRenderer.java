package com.freya02.emojis;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Transform;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * <b>You need to install dependencies in order to use this class</b>
 * <ul>
 *     <li>org.openjfx:javafx-graphics:18-ea+1</li>
 *     <li>org.openjfx:javafx-swing:18-ea+1</li>
 * </ul>
 * <p>
 * Provides utilities to render an {@link Emoji} at any size and with any background color, on either an AWT {@link BufferedImage}, or a JavaFX {@link Image}
 * <br><b>It is recommended you reuse this instance on a single thread as to limit resource usage</b>
 */
public class EmojiRenderer {
	static {
		PlatformImpl.startup(() -> {}, false);
	}

	private String url;

//	private WritableImage image;
	private Color backgroundColor = Color.TRANSPARENT;
	private int size = 72;

	/**
	 * Sets the {@link Emoji} to render
	 *
	 * @param emoji The {@link Emoji} to render
	 * @return This instance for chaining purposes
	 */
	public EmojiRenderer setEmoji(Emoji emoji) {
		url = emoji.getTwemojiImageUrl(TwemojiType.SVG);

		return this;
	}

	/**
	 * Sets the render size
	 * <br>The render size is only <b>best effort</b> and <b>does not</b> mean the emoji will be at this size
	 * <br>Though, the difference is only gonna be +-1 pixel on the width/height
	 * <br>Additionally, <b>rendered emojis are not square</b>
	 *
	 * @param size The render size
	 * @return This instance for chaining purposes
	 */
	public EmojiRenderer setSize(int size) {
		this.size = size;

		return this;
	}

	/**
	 * Sets the background color of this renderer
	 *
	 * @param backgroundColor The color to use as a background
	 * @return This instance for chaining purposes
	 */
	public EmojiRenderer setBackgroundColor(Color backgroundColor) {
		this.backgroundColor = backgroundColor;

		return this;
	}

	/**
	 * Sets the background color of this renderer
	 *
	 * @param backgroundColor The color to use as a background
	 * @return This instance for chaining purposes
	 */
	public EmojiRenderer setBackgroundColor(java.awt.Color backgroundColor) {
		this.backgroundColor = Color.rgb(backgroundColor.getRed(),
				backgroundColor.getGreen(),
				backgroundColor.getBlue(),
				backgroundColor.getAlpha() / 255.0);

		return this;
	}

//	May be unsafe
//	public EmojiRenderer setImage(WritableImage image) {
//		this.image = image;
//
//		return this;
//	}

	/**
	 * Returns an action which will render the emoji with the specified parameters
	 *
	 * @return An {@link Action} to render this emoji
	 */
	public Action<Image> render() {
		return new ActionImpl<>(this::doRender);
	}

	@Nullable
	private synchronized Image doRender() { //Prevent multiple threads from using this class as the fields could change in-between
		if (url == null) {
			throw new IllegalStateException("No emoji to be rendered");
		}

		final CompletableFuture<Object> future = new CompletableFuture<>();

		try {
			final List<Shape> shapes = getShapes();

			Platform.runLater(() -> {
				try {
					final AnchorPane pane = new AnchorPane();
					pane.getChildren().addAll(shapes);

					new Scene(pane);

					final SnapshotParameters params = new SnapshotParameters();
					params.setFill(backgroundColor);
					params.setTransform(Transform.scale(size / 36.0, size / 36.0));

					future.complete(pane.snapshot(params, null));
				} catch (Throwable e) {
					future.completeExceptionally(new RuntimeException("Unable to render SVG of url " + url, e));
				}
			});

			return (Image) future.get();
		} catch (Exception e) {
			throw new RuntimeException("Unable to render SVG of url " + url, e);
		}
	}

	@NotNull
	private List<Shape> getShapes() throws IOException {
		final Call call = HttpUtils.CLIENT.newCall(new Request.Builder()
				.url(url)
				.build());

		try (Response response = call.execute()) {
			if (response.isSuccessful() || response.isRedirect()) {
				return FXUtils.getSvgs(response.body().byteStream());
			} else {
				throw new IOException("Response code: " + response.code());
			}
		}
	}

	/**
	 * Returns an action which will render the emoji with the specified parameters
	 *
	 * @return An {@link Action} to render this emoji
	 */
	public Action<BufferedImage> renderAwt() {
		return render().flatMap(img -> SwingFXUtils.fromFXImage(img, null));
	}

	/**
	 * Returns an action which will render the emoji with the specified parameters
	 *
	 * @return An {@link Action} to render this emoji
	 */
	public Action<byte[]> renderBytes() {
		return renderAwt().flatMap(img -> {
			try {
				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				final boolean written = ImageIO.write(img, "png", baos);
				if (!written)
					throw new IOException("PNG couldn't be written!");

				return baos.toByteArray();
			} catch (IOException e) {
				throw new RuntimeException("Unable to render emoji to PNG bytes", e);
			}
		});
	}
}