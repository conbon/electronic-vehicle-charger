package evc;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import easicharge.conal.com.evc.R;

/**
 * Created by Conal McLaughlin on 08/03/15.
 */
public class About extends Activity {

    private Button aboutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.about);

        aboutButton = (Button) findViewById(R.id.okButton);

        aboutButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                finish();
            }
        });
    }
}
