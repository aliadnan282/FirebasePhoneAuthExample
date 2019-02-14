package com.nsikakthompson.firebasephoneauthexample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

import java.util.Locale;

import io.michaelrocks.libphonenumber.android.NumberParseException;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import io.michaelrocks.libphonenumber.android.Phonenumber;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    public static final int RC_SIGN_IN = 001;
    private static final String TAG = MainActivity.class.getSimpleName();
    private PhoneNumberUtil phoneNumberUtil = null;

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    private boolean mVerificationInProgress = false;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    private EditText phoneNumberField, smsCodeVerificationField;
    private Button startVerficationButton, verifyPhoneButton;

    private String verificationid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        //intialized firebase auth
        mAuth = FirebaseAuth.getInstance();

        phoneNumberField = findViewById(R.id.phone_number_edt);
        smsCodeVerificationField = findViewById(R.id.sms_code_edt);
        startVerficationButton = findViewById(R.id.start_auth_button);
        verifyPhoneButton = findViewById(R.id.verify_auth_button);

        startVerficationButton.setOnClickListener(this);
        verifyPhoneButton.setOnClickListener(this);


    }

    @Override
    protected void onStart() {
        super.onStart();

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {

                signInWithPhoneAuthCredential(phoneAuthCredential);

            }

            @Override
            public void onVerificationFailed(FirebaseException e) {

                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);

                verificationid = s;
                verifyPhoneButton.setVisibility(View.VISIBLE);
            }
        };
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        // Use the library’s functions
        phoneNumberUtil = PhoneNumberUtil.createInstance(getApplicationContext());
        Phonenumber.PhoneNumber phNumberProto = null;
        try {
            TelephonyManager tm = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
            String countryCodeValue = tm.getNetworkCountryIso();
            Phonenumber.PhoneNumber example=phoneNumberUtil.getExampleNumber(countryCodeValue);
            // I set the default region to PH (Philippines)
            // You can find your country code here http://www.iso.org/iso/country_names_and_code_elements
            phNumberProto = phoneNumberUtil.parse(phoneNumber, "PK");

        } catch (NumberParseException e) {
            // if there’s any error
           e.printStackTrace();//("NumberParseException was thrown: " + e.toString());
        }

        // check if the number is valid
        boolean isValid = phoneNumberUtil.isValidNumber(phNumberProto);
        if (isValid) {

            // get the valid number’s international format
            String internationalFormat = phoneNumberUtil.format(
                    phNumberProto,
                    PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);

            Toast.makeText(
                    getBaseContext(),
                    "Phone number VALID: "+internationalFormat,
                    Toast.LENGTH_SHORT).show();

        } else {

            // prompt the user when the number is invalid
            Toast.makeText(
                    getBaseContext(),
                    "Phone number is INVALID: "+phoneNumber,
                    Toast.LENGTH_SHORT).show();

        }
       /* PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks*/


    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }


    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");

                            FirebaseUser user = task.getResult().getUser();

                            startActivity(new Intent(getApplicationContext(), Main2Activity.class));

                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid

                                smsCodeVerificationField.setError("Invalid code.");

                            }

                        }
                    }
                });
    }

    private boolean validatePhoneNumberAndCode() {
        String phoneNumber = phoneNumberField.getText().toString();
        if (TextUtils.isEmpty(phoneNumber)) {
            phoneNumberField.setError("Invalid phone number.");
            return false;
        }


        return true;
    }

    private boolean validateSMSCode() {
        String code = smsCodeVerificationField.getText().toString();
        if (TextUtils.isEmpty(code)) {
            smsCodeVerificationField.setError("Enter verification Code.");
            return false;
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {

        int id = view.getId();
        switch (id) {
            case R.id.start_auth_button:
                if (!validatePhoneNumberAndCode()) {
                    return;
                }
                startPhoneNumberVerification(phoneNumberField.getText().toString());
                break;
            case R.id.verify_auth_button:
                if (!validateSMSCode()) {
                    return;
                }

                verifyPhoneNumberWithCode(verificationid, smsCodeVerificationField.getText().toString());
                break;
        }

    }
}
