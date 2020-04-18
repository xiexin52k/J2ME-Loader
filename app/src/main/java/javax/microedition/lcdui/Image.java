/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2017-2018 Nikita Shakarun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.microedition.lcdui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.LruCache;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.lcdui.game.Sprite;
import javax.microedition.util.ContextHolder;

import ru.playsoftware.j2meloader.util.PNGUtils;

public class Image {

	private static final int CACHE_SIZE = (int) (Runtime.getRuntime().maxMemory() >> 2); // 1/4 heap max
	private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(CACHE_SIZE) {
		@Override
		protected int sizeOf(String key, Bitmap value) {
			return value.getByteCount();
		}
	};

	private Bitmap bitmap;
	private Canvas canvas;

	public Image(Bitmap bitmap) {
		if (bitmap == null) {
			throw new NullPointerException();
		}

		this.bitmap = bitmap;
	}

	public static Image createImage(int width, int height, Image reuse) {
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		if (reuse == null) {
			return new Image(bitmap);
		}
		reuse.getCanvas().setBitmap(bitmap);
		reuse.copyPixels(reuse);
		reuse.bitmap = bitmap;
		return new Image(bitmap);
	}

	public Bitmap getBitmap() {
		return bitmap;
	}

	public Canvas getCanvas() {
		if (canvas == null) {
			canvas = new Canvas(bitmap);
		}

		return canvas;
	}

	public static Image createImage(int width, int height) {
		Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		b.eraseColor(Color.WHITE);
		return new Image(b);
	}

	public static Image createImage(String resname) throws IOException {
		synchronized (CACHE) {
			Bitmap b = CACHE.get(resname);
			if (b != null) {
				return new Image(b);
			}
			InputStream stream = ContextHolder.getResourceAsStream(null, resname);
			if (stream == null) {
				throw new IOException("Can't read image: " + resname);
			}
			b = PNGUtils.getFixedBitmap(stream);
			stream.close();
			if (b == null) {
				throw new IOException("Can't decode image: " + resname);
			}
			CACHE.put(resname, b);
			return new Image(b);
		}
	}

	public static Image createImage(InputStream stream) throws IOException {
		Bitmap b = PNGUtils.getFixedBitmap(stream);
		if (b == null) {
			throw new IOException("Can't decode image");
		}
		return new Image(b);
	}

	public static Image createImage(byte[] imageData, int imageOffset, int imageLength) {
		Bitmap b = PNGUtils.getFixedBitmap(imageData, imageOffset, imageLength);
		if (b == null) {
			throw new IllegalArgumentException("Can't decode image");
		}
		return new Image(b);
	}

	public static Image createImage(Image image, int x, int y, int width, int height, int transform) {
		return new Image(Bitmap.createBitmap(image.bitmap, x, y, width, height, Sprite.transformMatrix(transform, width / 2f, height / 2f), false));
	}

	public static Image createImage(Image image) {
		return new Image(Bitmap.createBitmap(image.bitmap));
	}

	public static Image createRGBImage(int[] rgb, int width, int height, boolean processAlpha) {
		Bitmap b = Bitmap.createBitmap(rgb, width, height, Bitmap.Config.ARGB_8888);
		b.setHasAlpha(processAlpha);
		return new Image(b);
	}

	public Graphics getGraphics() {
		Graphics graphics = new Graphics();
		graphics.setCanvas(new Canvas(bitmap), bitmap);
		return graphics;
	}

	public boolean isMutable() {
		return bitmap.isMutable();
	}

	public int getWidth() {
		return bitmap.getWidth();
	}

	public int getHeight() {
		return bitmap.getHeight();
	}

	public void getRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height) {
		bitmap.getPixels(rgbData, offset, scanlength, x, y, width, height);
	}

	void copyPixels(Image dst) {
		dst.getCanvas().drawBitmap(bitmap, 0, 0, null);
	}
}
