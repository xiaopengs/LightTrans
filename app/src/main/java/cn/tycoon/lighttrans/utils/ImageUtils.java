package cn.tycoon.lighttrans.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class ImageUtils {
	
    private static final int STROKE_WIDTH = 6;

    public static Bitmap processRoundBitmap(Bitmap bitmap) {
        int x, y;
        int radius;
        Bitmap bitmapEx;
        Canvas canvas;
        Paint paint = new Paint();
        final PorterDuffXfermode mXfermodeSrcIn = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);

        bitmapEx = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmapEx);
        radius = bitmapEx.getHeight() / 2;
        x = bitmapEx.getWidth() / 2 - radius;
        y = bitmapEx.getHeight() / 2 - radius;

        paint.setColor(0xFFFFFFFF);
        paint.setAntiAlias(true);
        canvas.drawOval(new RectF(x, y, x + 2 * radius, y + 2 * radius), paint);
        paint.setAntiAlias(false);
        paint.setXfermode(mXfermodeSrcIn);
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return bitmapEx;
    }
    
    public static Bitmap transform(Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());

        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;

        Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
        if (squaredBitmap != source) {
            source.recycle();
        }

        Bitmap bitmap = Bitmap.createBitmap(size, size, source.getConfig());

        Canvas canvas = new Canvas(bitmap);

        Paint avatarPaint = new Paint();
        BitmapShader shader = new BitmapShader(squaredBitmap, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
        //avatarPaint.setColor(Color.parseColor("#7FDADB"));
        avatarPaint.setShader(shader);

        Paint outlinePaint = new Paint();
        outlinePaint.setColor(Color.WHITE);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(STROKE_WIDTH);
        outlinePaint.setAntiAlias(true);

        float r = size / 2f;
        canvas.drawCircle(r, r, r, avatarPaint);
        canvas.drawCircle(r, r, r - STROKE_WIDTH / 2, outlinePaint);

        squaredBitmap.recycle();
        return bitmap;
    }

    
	/**
	 *
	 * @param drawable
	 * @return
	 */
	public static Bitmap drawableToBitmap(Drawable drawable) {
		int width = drawable.getIntrinsicWidth();
		int height = drawable.getIntrinsicHeight();
		Bitmap bitmap = Bitmap.createBitmap(width, height, drawable
				.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
				: Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, width, height);
		drawable.draw(canvas);
		return bitmap;

	}
	
	/**
	 * 
	 * @param bitmap
	 * @return
	 */
	public static Drawable bitmapToDrawable(Bitmap bitmap) {
		Drawable drawable = new BitmapDrawable(bitmap);
		return drawable;
	}
	
	
}
