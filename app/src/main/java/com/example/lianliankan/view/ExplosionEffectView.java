package com.example.lianliankan.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ExplosionEffectView extends View {

    private static final int PARTICLE_COUNT = 18;
    private static final long DURATION_MS = 420L;
    private final List<Particle> particles = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();

    public ExplosionEffectView(Context context) {
        super(context);
        init();
    }

    public ExplosionEffectView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ExplosionEffectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        setClickable(false);
    }

    public void explodeAt(float x, float y) {
        int[] colors = {0xFFFF9800, 0xFFFFD54F, 0xFF16A34A, 0xFF38BDF8};
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double angle = (Math.PI * 2 * i / PARTICLE_COUNT) + random.nextDouble() * 0.28;
            float distance = 42f + random.nextFloat() * 52f;
            Particle particle = new Particle();
            particle.startX = x;
            particle.startY = y;
            particle.endX = x + (float) Math.cos(angle) * distance;
            particle.endY = y + (float) Math.sin(angle) * distance;
            particle.radius = 4f + random.nextFloat() * 6f;
            particle.color = colors[i % colors.length];
            particles.add(particle);
            animateParticle(particle);
        }
        invalidate();
    }

    private void animateParticle(Particle particle) {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(DURATION_MS);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            particle.progress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                particles.remove(particle);
                invalidate();
            }
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle particle = iterator.next();
            float progress = particle.progress;
            if (progress >= 1f) {
                iterator.remove();
                continue;
            }
            float x = lerp(particle.startX, particle.endX, progress);
            float y = lerp(particle.startY, particle.endY, progress);
            paint.setColor(particle.color);
            paint.setAlpha((int) (255 * (1f - progress)));
            canvas.drawCircle(x, y, particle.radius * (1f - progress * 0.45f), paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    private float lerp(float start, float end, float progress) {
        return start + (end - start) * progress;
    }

    private static class Particle {
        float startX;
        float startY;
        float endX;
        float endY;
        float radius;
        float progress;
        int color;
    }
}
