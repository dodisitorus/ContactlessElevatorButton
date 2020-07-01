package com.dodi.contactlesselevatorbutton;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dodi.contactlesselevatorbutton.service.ApiClient;
import com.dodi.contactlesselevatorbutton.service.GetService;
import com.dodi.contactlesselevatorbutton.service.PhotoData;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonObject;
import com.ibm.wiotp.sdk.app.ApplicationClient;
import com.ibm.wiotp.sdk.codecs.JsonCodec;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ControlFragment extends Fragment {

    private TextView status_item_down, status_item_up, label_up, label_down;
    private RelativeLayout layoutDown, layoutUp, remoteDown, remoteUp;

    private FirebaseDatabase database;
    private DatabaseReference myRef;

    private String elevatorUp;
    private String elevatorDown;

    private String remoteUpFinal;
    private String remoteDownFinal;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_control, container, false);

        status_item_down = view.findViewById(R.id.status_item_down);
        status_item_up = view.findViewById(R.id.status_item_up);

        label_up = view.findViewById(R.id.label_up);
        label_down = view.findViewById(R.id.label_down);

        layoutUp = view.findViewById(R.id.item_layout_up);
        layoutDown = view.findViewById(R.id.item_layout_down);

        remoteUp = view.findViewById(R.id.item_layout_remote_up);
        remoteDown = view.findViewById(R.id.item_layout_remote_down);

        layoutUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (elevatorUp.equals("true")) {
                    myRef.child("elevatorUp").child("isActive").setValue("false");
                } else {
                    myRef.child("elevatorUp").child("isActive").setValue("true");
                }
            }
        });

        layoutDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (elevatorDown.equals("true")) {
                    myRef.child("elevatorDown").child("isActive").setValue("false");
                } else {
                    myRef.child("elevatorDown").child("isActive").setValue("true");
                }
            }
        });

        remoteUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myRef.child("remoteUp").setValue("true");
                myRef.child("remoteDown").setValue("false");

                upSetteled();
            }
        });

        remoteDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myRef.child("remoteDown").setValue("true");
                myRef.child("remoteUp").setValue("false");

                downSetteled();
            }
        });

        return view;
    }

    private void upSetteled() {
        remoteUp.setBackgroundColor(getResources().getColor(R.color.profilePrimaryDark));
        remoteDown.setBackgroundColor(getResources().getColor(R.color.whiteCardColor));
        label_up.setTextColor(getResources().getColor(R.color.whiteCardColor));
        label_down.setTextColor(getResources().getColor(R.color.profileEditTextColor));
    }

    private void downSetteled() {
        remoteDown.setBackgroundColor(getResources().getColor(R.color.profilePrimaryDark));
        remoteUp.setBackgroundColor(getResources().getColor(R.color.whiteCardColor));
        label_down.setTextColor(getResources().getColor(R.color.whiteCardColor));
        label_up.setTextColor(getResources().getColor(R.color.profileEditTextColor));
    }

    private void resetButtonRemote() {
        remoteUp.setBackgroundColor(getResources().getColor(R.color.whiteCardColor));
        label_up.setTextColor(getResources().getColor(R.color.profileEditTextColor));
        remoteDown.setBackgroundColor(getResources().getColor(R.color.whiteCardColor));
        label_down.setTextColor(getResources().getColor(R.color.profileEditTextColor));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("elevator");
        myRef.child("remoteDown").setValue("false");
        myRef.child("remoteUp").setValue("false");

        getElevatorStatus();

        getRealTimeData();

        try {
            connectingIBMIoTWatson();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void getRealTimeData() {
        GetService service = ApiClient.getRetrofitInstance().create(GetService.class);
        Call<List<PhotoData>> call = service.getAllPhotos();
        call.enqueue(new Callback<List<PhotoData>>() {
            @Override
            public void onResponse(Call<List<PhotoData>> call, Response<List<PhotoData>> response) {
                Toast.makeText(getContext(), "" +  response.body().get(0).getTitle(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<List<PhotoData>> call, Throwable t) {
                Toast.makeText(getContext(), t.getMessage() + ", " + t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void connectingIBMIoTWatson() throws Exception {

        ApplicationClient applicationClient = new ApplicationClient();
        applicationClient.registerCodec(new JsonCodec());
        applicationClient.connect();
        JsonObject data = new JsonObject();
        data.addProperty("distance", 10);
        applicationClient.publishEvent("RaspberryPiSensor", "RaspberryPiSensor01", "psutil", data);
        applicationClient.disconnect();
    }

    private void getElevatorStatus() {
        myRef.addValueEventListener(valueEventListener);
    }

    private ValueEventListener valueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            elevatorUp = dataSnapshot.child("elevatorUp").child("isActive").getValue(String.class);
            elevatorDown = dataSnapshot.child("elevatorDown").child("isActive").getValue(String.class);

            if (elevatorUp.equals("true")) {
                status_item_up.setText(R.string.is_on);
            } else {
                status_item_up.setText(R.string.is_off);
            }

            if (elevatorDown.equals("true")) {
                status_item_down.setText(R.string.is_on);
            } else {
                status_item_down.setText(R.string.is_off);
            }

            String stateUp = dataSnapshot.child("remoteUp").getValue(String.class);
            String stateDown = dataSnapshot.child("remoteDown").getValue(String.class);

            if (stateUp.equals("true") && stateDown.equals("false")) {
                upSetteled();
            } else if (stateUp.equals("false") && stateDown.equals("true")) {
                downSetteled();
            } else {
                resetButtonRemote();
            }
        }

        @Override
        public void onCancelled(DatabaseError error) {
            // Failed to read value
            Log.w("DatabaseError", "Failed to read value.", error.toException());
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }
}
