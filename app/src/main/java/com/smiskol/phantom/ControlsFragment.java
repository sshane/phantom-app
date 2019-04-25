package com.smiskol.phantom;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class ControlsFragment extends Fragment {
    CustomSeekBar steerSeekBar;
    TextView steerTextView;
    LinearLayout holdButton;
    ImageButton speedPlusButton;
    ImageButton speedSubButton;
    TextView speedTextView;



    private OnFragmentInteractionListener mListener;

    public ControlsFragment() {
        // Required empty public constructor
    }


    public static ControlsFragment newInstance(String param1, String param2) {
        ControlsFragment fragment = new ControlsFragment();
        return fragment;
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
        setUpListeners();
        return view;
    }

    public void setUpListeners(){
        steerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                steerTextView.setText(-(progress - 100) + "°");
                ((MainActivity) getActivity()).steeringAngle = -(progress - 100);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        speedPlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).desiredSpeed = Math.min(((MainActivity) getActivity()).desiredSpeed + 0.5, 10);
                speedTextView.setText(String.valueOf(((MainActivity) getActivity()).desiredSpeed) + " mph");
            }
        });
        speedSubButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).desiredSpeed = Math.max(((MainActivity) getActivity()).desiredSpeed - 0.5, 2);
                speedTextView.setText(String.valueOf(((MainActivity) getActivity()).desiredSpeed) + " mph");
            }
        });
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
