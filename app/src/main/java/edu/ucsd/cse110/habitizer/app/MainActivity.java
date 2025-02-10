package edu.ucsd.cse110.habitizer.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        var ld = new MutableLiveData<String>();
        ld.observe(this, (s) -> {
            System.out.println(s);
        });
    }
}