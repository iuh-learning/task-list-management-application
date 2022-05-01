package fit.android.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fit.android.app.R;
import fit.android.app.dao.ItemDetailListDAO;
import fit.android.app.dao.ItemTaskListDAO;
import fit.android.app.dao.UserDAO;
import fit.android.app.database.AppDatabase;
import fit.android.app.fragment.FragmentDetailTaskList;
import fit.android.app.fragment.FragmentItemTaskList;
import fit.android.app.helper.Message;
import fit.android.app.helper.MessageBoxListener;
import fit.android.app.model.ItemDetailList;
import fit.android.app.model.ItemTaskList;

public class MainActivity_DetailList extends AppCompatActivity {

    // init
    private TextView tvNameTask;
    private TextView tvEdit;
    private TextView tvBackToList;
    private Button btnADD;
    private Button btnUPDATE;

    private AppDatabase db;
    private ItemDetailListDAO dao;

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_your_detail);

        // Render --> UI Listview (reload listview)
        reLoadListView();

        // SQLite
        db = AppDatabase.getDatabase(MainActivity_DetailList.this);

        // DAO
        dao = db.itemDetailListDAO();
        // find id
        tvNameTask = findViewById(R.id.txtNameTask);
        tvEdit = findViewById(R.id.txtEdit);
        tvBackToList = findViewById(R.id.txtBackToList);
        btnUPDATE = findViewById(R.id.btnUPDATE);
        btnADD = findViewById(R.id.btnADD);

        // Nhận name task from page Task List
        Intent intent = getIntent();
        String nameTask = intent.getStringExtra("name_task");
        tvNameTask.setText(nameTask);

        //Nhận mail
        String mail = intent.getStringExtra("user_email");
        // Nhận name
        ItemDetailList itemDetailList = getNameTaskFromIntent();
        if (itemDetailList != null) {
            tvEdit.setText(itemDetailList.getNameDetail());
        }

        // Nhận taskID
        Bundle bundle = getIntent().getExtras();
        int taskID = bundle.getInt("id_task");

        //add
        btnADD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String txtEdit = tvEdit.getText().toString().trim();
                // check null
                if (txtEdit.isEmpty()) {
                    Message.showMessage(MainActivity_DetailList.this, "Message", "Task detail not null!");
                    return;
                }
                // check name detail
                if (dao.findByNameTask(txtEdit) != null) {
                    Message.showMessage(MainActivity_DetailList.this, "Message", "Task detail already exists!");
                    return;
                }
                dao.insert(new ItemDetailList(txtEdit, taskID));
                saveDataFromClientToFireBas(taskID,mail);
                reLoadListView();
                clearInput();
            }
        });

        //Update
        btnUPDATE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String txtEdit = tvEdit.getText().toString().trim();
                // check null
                if (txtEdit.isEmpty()) {
                    Message.showMessage(MainActivity_DetailList.this, "Message", "Task detail not null!");
                    return;
                }
                // check name detail
                if (dao.findByNameTask(txtEdit) != null) {
                    Message.showMessage(MainActivity_DetailList.this, "Message", "Task detail already exists!");
                    return;
                }
                Message.showConfirmMessgae(MainActivity_DetailList.this, "Message", "Do you want to update?", new MessageBoxListener() {
                    @Override
                    public void result(int result) {
                        if (result == 1) {
                            itemDetailList.setNameDetail(txtEdit);
                            dao.update(itemDetailList);
                            reLoadListView();
                            clearInput();
                        }
                    }
                });
            }
        });

        tvBackToList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent1 = new Intent(MainActivity_DetailList.this, MainActivity_TaskList.class);
                Bundle bundle = new Bundle();
                bundle.putString("user_email", mail);
                intent1.putExtras(bundle);
                startActivity(intent1);
            }
        });

    }

    private void clearInput() {
        tvEdit.setText("");
        tvEdit.requestFocus();
    }

    // reload listview
    public void reLoadListView() {
        FragmentDetailTaskList fragmentDetailTaskList = new FragmentDetailTaskList();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.Fragment_detail, fragmentDetailTaskList, "Fragment item task-detail.")
                .commit();
    }

    // get id task detail from intent
    private ItemDetailList getNameTaskFromIntent() {
        Bundle bundle = getIntent().getExtras();
        int taskID1 = bundle.getInt("id_taskDetail");

        ItemDetailList itemDetailList = dao.findByIDTask(taskID1);

        return itemDetailList;
    }

    // save data to firebase
    private void saveDataFromClientToFireBas(int taskID, String mail) {
        ItemDetailListDAO itemDetailListDAO = AppDatabase.getDatabase(this).itemDetailListDAO();
        List<ItemDetailList> list = dao.getAll(taskID);
        Map<String, String> mapUsers = new HashMap<>();
        UserDAO userDAO = AppDatabase.getDatabase(this).userDAO();
        int idUser = userDAO.findByEmail(mail).getId();
        mDatabase = FirebaseDatabase.getInstance().getReference("/users/"+idUser+"/list-task/"+taskID);

        for (ItemDetailList item : list) {
            mapUsers.put("" + item.getId(), item.getNameDetail());
        }

        try {
            mDatabase.child("detail-task").setValue(mapUsers);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
