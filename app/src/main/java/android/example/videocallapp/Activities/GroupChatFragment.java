package android.example.videocallapp.Activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.example.videocallapp.Adapters.GroupMessagesAdapter;
import android.example.videocallapp.Adapters.MessagesAdapter;
import android.example.videocallapp.Models.Message;
import android.example.videocallapp.R;
import android.example.videocallapp.databinding.ActivityChatBinding;
import android.example.videocallapp.databinding.FragmentGroupChatBinding;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class GroupChatFragment extends Fragment {


    private GroupMessagesAdapter adapter;
    private ArrayList<Message> messages;

    private FirebaseDatabase database;
    private FirebaseStorage storage;

    private FragmentGroupChatBinding binding;

    private ProgressDialog dialog;

    private String senderUid;

    public GroupChatFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Getting reference  of firebase varibles
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        senderUid = FirebaseAuth.getInstance().getUid();

        //Creating binding to get access of the layout variables
        binding = FragmentGroupChatBinding.inflate(
                inflater);
        View view = binding.getRoot();

        //Showing dialog box while uploading the image
        dialog = new ProgressDialog(container.getContext());
        dialog.setMessage("Uploading Image.....");
        dialog.setCancelable(false);

        //Creating an Arraylist of messages
        messages = new ArrayList<>();
        adapter = new GroupMessagesAdapter(container.getContext() , messages);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(container.getContext()));
        binding.recyclerView.setAdapter(adapter);

        //Setting onclick listener to send button
        binding.sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Gettign the message that has been typed
                String messageText = binding.messageBox.getText().toString();

                //Geting time at of message sent
                Date date = new Date();
                Message message = new Message(messageText , senderUid , date.getTime());
                binding.messageBox.setText("");
                database.getReference()
                        .child("Public")
                        .push()
                        .setValue(message);
            }
        });

        //Storing thew messages in firebase database
        database.getReference()
                .child("Public")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messages.clear();
                        for (DataSnapshot snapshot1:snapshot.getChildren()){
                            Message message = snapshot1.getValue(Message.class);
                            message.setMessageId(snapshot1.getKey());
                            messages.add(message);
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        //Added onclick listener to the attachment for sending photos from phone gallery
        binding.attachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent , 20);
            }
        });

        return view;
    }


    //Getting image from gallery
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 20){
            if(data != null){
                if(data.getData() != null){
                    //Uri of the image selected
                    Uri selectedImage = data.getData();
                    Calendar calendar = Calendar.getInstance();
                    //Storing in storage of database
                    StorageReference reference = storage.getReference().child("Chats").child(calendar.getTimeInMillis() + "");
                    dialog.show();
                    reference.putFile(selectedImage).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            dialog.dismiss();
                            if(task.isSuccessful()){
                                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        String filepath = uri.toString();
                                        String messageText = binding.messageBox.getText().toString();
                                        //Pushing the message in the firebase
                                        Date date = new Date();
                                        Message message = new Message(messageText , senderUid , date.getTime());
                                        message.setMessage("photo");
                                        message.setImageUrl(filepath);
                                        binding.messageBox.setText("");

                                        //storing the message in data base
                                        database.getReference()
                                                .child("Public")
                                                .push()
                                                .setValue(message);
                                    }
                                });
                            }
                        }
                    });
                }
            }
        }
    }
}