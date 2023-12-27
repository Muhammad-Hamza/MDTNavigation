package com.karwa.mdtnavigation;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.NoSuchElementException;

/**
 * Created by Oashraf on 5/15/2016.
 */
public class ImageUtil
{
    public static void scaleImage(ImageView view) throws NoSuchElementException
    {
        // Get bitmap from the the ImageView.
        Bitmap bitmap = null;

        try
        {
            Drawable drawing = view.getDrawable();
            bitmap = ((BitmapDrawable) drawing).getBitmap();
        } catch (NullPointerException e)
        {
            throw new NoSuchElementException("No drawable on given view");
        } catch (ClassCastException e)
        {
            // Check bitmap is Ion drawable
            //            bitmap = Ion.with(view).getBitmap();
            e.printStackTrace();
        }

        // Get current dimensions AND the desired bounding box
        int width = 0;

        try
        {
            width = bitmap.getWidth();
        } catch (NullPointerException e)
        {
            throw new NoSuchElementException("Can't find bitmap on given view/drawable");
        }

        int height = bitmap.getHeight();
        int bounding = dpToPx(250);
        //        Log.i("Test", "original width = " + Integer.toString(width));
        //        Log.i("Test", "original height = " + Integer.toString(height));
        //        Log.i("Test", "bounding = " + Integer.toString(bounding));

        // Determine how much to scale: the dimension requiring less scaling is
        // closer to the its side. This way the image always stays inside your
        // bounding box AND either x/y axis touches it.
        float xScale = ((float) bounding) / width;
        float yScale = ((float) bounding) / height;
        float scale = (xScale <= yScale) ? xScale : yScale;
        //        Log.i("Test", "xScale = " + Float.toString(xScale));
        //        Log.i("Test", "yScale = " + Float.toString(yScale));
        //        Log.i("Test", "scale = " + Float.toString(scale));

        // Create a matrix for the scaling and add the scaling data
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        // Create a new bitmap and convert it to a format understood by the ImageView
        Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        width = scaledBitmap.getWidth(); // re-use
        height = scaledBitmap.getHeight(); // re-use
        BitmapDrawable result = new BitmapDrawable(scaledBitmap);
        //        Log.i("Test", "scaled width = " + Integer.toString(width));
        //        Log.i("Test", "scaled height = " + Integer.toString(height));

        // Apply the scaled bitmap
        view.setImageDrawable(result);

        // Now change ImageView's dimensions to match the scaled image
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        params.width = width;
        params.height = height;
        view.setLayoutParams(params);

        //        Log.i("Test", "done");
    }

    public static int dpToPx(int dp)
    {
        float density = ApplicationStateData.INSTANCE.getApplicationContext().getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}
