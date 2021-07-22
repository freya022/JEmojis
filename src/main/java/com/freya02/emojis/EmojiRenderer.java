package com.freya02.emojis;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Transform;
import okhttp3.Call;
import okhttp3.OkHttpClient;
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

public class EmojiRenderer {
	private static final OkHttpClient client = new OkHttpClient.Builder().build();

	static {
		PlatformImpl.startup(() -> {}, false);
	}

	private String url;

	private WritableImage image;
	private Color backgroundColor = Color.TRANSPARENT;
	private int size = 72;

	public EmojiRenderer setEmoji(Emoji emoji) {
		url = emoji.getTwemojiImageUrl(TwemojiType.SVG);

		return this;
	}

	public EmojiRenderer setSize(int size) {
		this.size = size;

		return this;
	}

	public EmojiRenderer setBackgroundColor(Color backgroundColor) {
		this.backgroundColor = backgroundColor;

		return this;
	}

	public EmojiRenderer setImage(WritableImage image) {
		this.image = image;

		return this;
	}

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

					future.complete(pane.snapshot(params, image));
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
		final Call call = client.newCall(new Request.Builder()
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

	public Action<BufferedImage> renderAwt() {
		return render().flatMap(img -> SwingFXUtils.fromFXImage(img, null));
	}

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