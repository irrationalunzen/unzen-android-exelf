package unzen.android.test.cpp.exec;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import unzen.android.test.cpp.exec.cppmodule.CppModule;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView tv = findViewById(R.id.main_text);
        tv.setText(CppModule.stringFromJNI());
    }
}
