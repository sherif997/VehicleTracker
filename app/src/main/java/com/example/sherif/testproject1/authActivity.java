package com.example.sherif.testproject1;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by SHERIF on 28/01/2018.
 */

public class authActivity extends AppCompatActivity  {
    private FirebaseAuth mAuth;
    Button submitButton;      //Different xml attributes referred to, these will change depending on the users actions
    EditText phoneNumber;
    EditText authCode;
    TextView errorText;
    private String mVerificationId;
    PhoneAuthProvider.ForceResendingToken mResendToken;
    private int function=1;


     PhoneAuthProvider.OnVerificationStateChangedCallbacks callBack; //Create an object which monitors the callback status for the user e.g.whether the passcode has been sent

    @Override
    protected void onCreate(Bundle savedInstanceState) {//Initial state when the authentication screen is loaded
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authscreen);
      /*  mAuth=FirebaseAuth.getInstance();
        authAction(submitButton);*/
        //onStop();
        mAuth=FirebaseAuth.getInstance();
        phoneNumber=(EditText)findViewById(R.id.editText);
        authCode=(EditText)findViewById(R.id.editText2);
        submitButton=(Button)findViewById(R.id.submitButton);
        errorText=(TextView)findViewById(R.id.errorText);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//The action which monitors what happens when the submit button is pressed
                if(function==1) {
                    String phone = phoneNumber.getText().toString();
                    authCode.setVisibility(View.VISIBLE);  //When number is entered, display authentication code prompt
                    phoneNumber.setEnabled(false);
                    submitButton.setText("Submit");
                    String pin = authCode.getText().toString();
                    PhoneAuthProvider.getInstance().verifyPhoneNumber(
                            phone,
                            60,
                            TimeUnit.SECONDS,
                            authActivity.this,
                            callBack);
                }
                else{
                    submitButton.setEnabled(false);
                    String verification=authCode.getText().toString(); //Display error message and the page should remain the same
                    PhoneAuthCredential cred=PhoneAuthProvider.getCredential(mVerificationId,verification);
                    signInWithPhoneAuthCredential(cred);
                }
            }
        });
        callBack= new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                errorText.setVisibility(View.VISIBLE);// Error text if user is unsuccessful
            }
            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
               // Log.d(TAG, "onCodeSent:" + verificationId);
                function=0;
                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;


            }
        };
    }


    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) { //Method for the sign in process
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                           // Log.d(TAG, "signInWithCredential:success");
                            sendConfirmationSMS();
                            Intent maps=new Intent(authActivity.this, MapsActivity.class);
                            FirebaseUser user = task.getResult().getUser();
                            startActivity(maps);
                            // ...
                        } else {
                            errorText.setVisibility(View.VISIBLE);
                            // Sign in failed, display a message and update the UI
                           // Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                            }
                        }
                    }
                });
    }



    //TRY IT NOW!!!
    public void sendConfirmationSMS(){
        String sentMessage="Number authenticated";
        String number="+447387835492";
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)!= PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.SEND_SMS)){
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.SEND_SMS},1);
            }
            else{
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.SEND_SMS},1);
            }
        }
        else{

        }
        try {
            SmsManager manager = SmsManager.getDefault();
            Toast.makeText(this,"SMS authentication message sent ",Toast.LENGTH_SHORT).show();//Message should display to indicate rate is set
            ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.SEND_SMS},1);
            manager.sendTextMessage(number, null, sentMessage, null, null);
        }
        catch(Exception e){
            System.out.println("Message could not be sent");
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode){
            case 1:
                if (grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)==PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(this,"Permission granted!",Toast.LENGTH_SHORT).show();//Will display indicator showing that permission is granted
                    }
                }
                else{
                    Toast.makeText(this,"No permission granted!",Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }




}

