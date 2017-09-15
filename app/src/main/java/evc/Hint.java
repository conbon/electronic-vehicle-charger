package evc;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import easicharge.conal.com.evc.R;

/**
 * Created by conal on 25/01/15.
 */
public class Hint extends Activity {

    private Button mHintButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.hint);

        mHintButton = (Button) findViewById(R.id.button1);

        mHintButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                finish();
            }
        });
    }



}