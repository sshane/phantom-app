package com.smiskol.phantom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class ControlsFragment extends Fragment {
    CenterSeekBar steerSeekBar;
    TextView steerTextView;
    LinearLayout holdButton;
    ImageButton speedPlusButton;
    ImageButton speedSubButton;
    TextView speedTextView;
    MainActivity context;

    private OnFragmentInteractionListener mListener;

    public ControlsFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_controls, container, false);
        steerSeekBar = view.findViewById(R.id.steerSeekBarNew);
        steerTextView = view.findViewById(R.id.steerTextViewNew);
        speedPlusButton = view.findViewById(R.id.speedPlusButton);
        speedSubButton = view.findViewById(R.id.speedSubButton);
        holdButton = view.findViewById(R.id.holdButton);
        speedTextView = view.findViewById(R.id.speedTextView);
        context = ((MainActivity) getActivity());
        setUpListeners();
        setUpHoldButton();
        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setUpHoldButton() {
        holdButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    System.out.println("move button down");
                    TransitionDrawable transition = (TransitionDrawable) holdButton.getBackground();
                    transition.startTransition(175);
                    context.goDown = System.currentTimeMillis();
                    context.holdMessage = true;
                    context.buttonHeld = true;
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    System.out.println("move button up");
                    TransitionDrawable transition = (TransitionDrawable) holdButton.getBackground();
                    transition.reverseTransition(175);
                    context.holdMessage = false;
                    context.buttonHeld = false;
                    context.goDuration = System.currentTimeMillis() - context.goDown;
                    if (context.goDuration < 200) {
                        if (!(event.getAction() == MotionEvent.ACTION_CANCEL)) {
                            makeSnackbar("You must hold button down for acceleration!");
                        }
                    }
                    System.out.println("Button held for " + context.goDuration + " ms");
                }
                return false;
            }
        });
    }

    public void setUpListeners() {
        if (context.useMph) {
            speedTextView.setText(String.valueOf(context.desiredSpeed) + " mph");
        } else {
            speedTextView.setText(String.valueOf(context.desiredSpeed) + " km/h");
        }

        steerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                steerTextView.setText(-(progress - context.maxSteer) + "°");
                context.steeringAngle = -(progress - context.maxSteer);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                steerSeekBar.setMax(context.maxSteer * 2);
                steerSeekBar.setProgress(context.maxSteer / 2);
                context.trackingSteer = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                final Integer incrementBy = interp(context.maxSteer, 100, 360, 2, 4);
                final Integer threadLength;
                if (context.maxSteer >= 230) {
                    threadLength = 2;
                } else {
                    threadLength = 1;
                }
                context.trackingSteer = false;
                if (seekBar.getProgress() > context.maxSteer) {
                    final Integer endProgress = (steerSeekBar.getProgress() - context.maxSteer) / 2;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (int l = 0; l <= endProgress / threadLength; l++) {
                                steerSeekBar.setProgress(steerSeekBar.getProgress() - incrementBy);
                                try {
                                    Thread.sleep(2);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            steerSeekBar.setProgress(context.maxSteer);
                            context.steerLetGo = true;
                        }
                    }).start();
                } else if (seekBar.getProgress() < context.maxSteer) {
                    final Integer endProgress = (context.maxSteer - steerSeekBar.getProgress()) / 2;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (int l = 0; l <= endProgress / threadLength; l++) {
                                steerSeekBar.setProgress(steerSeekBar.getProgress() + incrementBy);
                                try {
                                    Thread.sleep(2);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            steerSeekBar.setProgress(context.maxSteer);
                            context.steerLetGo = true;
                        }
                    }).start();
                }
            }
        });
        speedPlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (context.useMph) {
                    context.desiredSpeed = Math.min(context.desiredSpeed + 2.0, 16);
                    speedTextView.setText(String.valueOf(context.desiredSpeed) + " mph");
                } else {
                    context.desiredSpeed = Math.min(context.desiredSpeed + 3.0, 26);
                    speedTextView.setText(String.valueOf(context.desiredSpeed) + " km/h");
                }
            }
        });
        speedSubButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (context.useMph) {
                    context.desiredSpeed = Math.max(context.desiredSpeed - 2.0, 0);
                    speedTextView.setText(String.valueOf(context.desiredSpeed) + " mph");
                } else {
                    context.desiredSpeed = Math.max(context.desiredSpeed - 3.0, 0);
                    speedTextView.setText(String.valueOf(context.desiredSpeed) + " km/h");
                }
            }
        });
    }

    public Integer interp(int value, int from1, int to1, int from2, int to2) {
        return (int) Math.round(((Double.valueOf(value)) - (Double.valueOf(from1))) / ((Double.valueOf(to1)) - (Double.valueOf(from1))) * ((Double.valueOf(to2)) - (Double.valueOf(from2))) + (Double.valueOf(from2)));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void makeSnackbar(String s) {
        Snackbar snackbar = Snackbar.make(getActivity().findViewById(android.R.id.content), s, Snackbar.LENGTH_SHORT);
        TextView tv = (snackbar.getView()).findViewById(android.support.design.R.id.snackbar_text);
        Typeface font = ResourcesCompat.getFont(getActivity(), R.font.product_regular);
        tv.setTypeface(font);
        snackbar.show();
    }


    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
