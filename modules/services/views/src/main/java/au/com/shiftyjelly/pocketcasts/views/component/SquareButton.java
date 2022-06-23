package au.com.shiftyjelly.pocketcasts.views.component;

import android.content.Context;
import androidx.appcompat.widget.AppCompatButton;
import android.util.AttributeSet;

public class SquareButton extends AppCompatButton {

    public SquareButton(final Context context) {
        super(context);
    }

    public SquareButton(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareButton(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        final int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        setMeasuredDimension(width, width);
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, w, oldw, oldh);
    }
}