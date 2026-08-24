package fyi.appy.inksend.giladkutiel

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.EditText

/** A plain EditText host, standing in for a third-party app's text field in IME instrumented tests. */
class TestHostActivity : Activity() {
    lateinit var editText: EditText
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editText = EditText(this).apply { id = View.generateViewId() }
        setContentView(editText)
    }
}
