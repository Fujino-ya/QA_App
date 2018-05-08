package jp.techacademy.youichi.satou.qa_app;

/**
 * Created by Fujino_ya on 2018/04/22.
 */

import android.content.Intent;

import android.support.design.widget.Snackbar;  // 課題
import android.widget.ImageButton;  // 課題

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.database.ValueEventListener;  // 課題

import java.util.HashMap;

public class QuestionDetailActivity extends AppCompatActivity implements View.OnClickListener {

    private ListView mListView;
    private Question mQuestion;
    private QuestionDetailListAdapter mAdapter;
    private DatabaseReference mAnswerRef;

    // --- 課題用メンバ --- //
    private DatabaseReference mFavoriteRef;
    private DatabaseReference mDatabaseReference;
    private FirebaseUser mUser;
    private boolean star = false;
    static final int RESULT_SUBACTIVITY = 1;
    ImageButton mFavoriteButton;


    private ChildEventListener mEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {

            HashMap map = (HashMap) dataSnapshot.getValue();

            String answerUid = dataSnapshot.getKey();

            for (Answer answer : mQuestion.getAnswers()) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid.equals(answer.getAnswerUid())) {
                    return;
                }
            }

            String body = (String) map.get("body");
            String name = (String) map.get("name");
            String uid = (String) map.get("uid");

            Answer answer = new Answer(body, name, uid, answerUid);
            mQuestion.getAnswers().add(answer);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_detail);

        // 渡ってきたQuestionのオブジェクトを保持する
        Bundle extras = getIntent().getExtras();
        mQuestion = (Question) extras.get("question");

        setTitle(mQuestion.getTitle());

        // ListViewの準備
        mListView = (ListView) findViewById(R.id.listView);
        mAdapter = new QuestionDetailListAdapter(this, mQuestion);
        mListView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();

        // --- 課題：ImageButton を設置し、Activity 作成時のお気に入りの判定、及び ImageResource の変更を行う --- //
        mFavoriteButton = (ImageButton) findViewById(R.id.favoriteButton);
        mFavoriteButton.setOnClickListener(this);

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mFavoriteRef = mDatabaseReference.child("favorite").child(mQuestion.getUid()).child(mQuestion.getQuestionUid());
        mFavoriteRef.addListenerForSingleValueEvent(new ValueEventListener(){
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                HashMap fav = (HashMap) dataSnapshot.getValue();

                if (fav != null) {
                    mFavoriteButton.setImageResource(R.drawable.pics2269);
                    star = true;
                } else {
                    mFavoriteButton.setImageResource(R.drawable.pics2274);
                    star = false;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        // --- ここまで ---- //

        // ログインの状態を確認する（課題の為に処理の位置を修正）
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        if (mUser == null) {
            mFavoriteButton.setVisibility(View.INVISIBLE);
        } else {
            mFavoriteButton.setVisibility(View.VISIBLE);
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // ログイン済みのユーザーを取得する
                //FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                if (mUser == null) {
                    // ログインしていなければログイン画面に遷移させる

                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivityForResult(intent, RESULT_SUBACTIVITY);
                } else {
                    // Questionを渡して回答作成画面を起動する

                    Intent intent = new Intent(getApplicationContext(), AnswerSendActivity.class);
                    intent.putExtra("question", mQuestion);
                    startActivity(intent);
                }
            }
        });
        DatabaseReference dataBaseReference = FirebaseDatabase.getInstance().getReference();
        mAnswerRef = dataBaseReference.child(Const.ContentsPATH).child(String.valueOf(mQuestion.getGenre())).child(mQuestion.getQuestionUid()).child(Const.AnswersPATH);
        mAnswerRef.addChildEventListener(mEventListener);
    }

    // ---課題：質問詳細画面にてログイン処理が実行された場合のお気に入りボタンの ImageResource の変更の為の処理 --- //
    // Activity を再描画する
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, requestCode, intent);

        if (resultCode == RESULT_OK && requestCode == RESULT_SUBACTIVITY && null != intent) {
            Intent intent1 = getIntent();
            overridePendingTransition(0, 0);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            finish();

            overridePendingTransition(0, 0);
            startActivity(intent1);
        }
    }
    // --- ここまで --- //

    // --- 課題：お気に入りボタンが押された場合の処理 --- //
    @Override
    public void onClick(View v) {
        View view = findViewById(android.R.id.content);
        //mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        DatabaseReference favoriteRef;

        if (star == true) {
            mFavoriteButton.setImageResource(R.drawable.pics2274);
            Snackbar.make(view, "お気に入りを解除しました", Snackbar.LENGTH_LONG).show();
            favoriteRef = mDatabaseReference.child(Const.FavoritePATH).child(mUser.getUid());
            HashMap<String, Object> data = new HashMap<String, Object>();
            data.put(String.valueOf(mQuestion.getQuestionUid()), null);
            favoriteRef.updateChildren(data);

            star = false;
        } else {
            mFavoriteButton.setImageResource(R.drawable.pics2269);
            Snackbar.make(view, "お気に入りに登録されました", Snackbar.LENGTH_LONG).show();
            favoriteRef = mDatabaseReference.child(Const.FavoritePATH).child(mUser.getUid()).child(mQuestion.getQuestionUid());
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("Genre", String.valueOf(mQuestion.getGenre()));
            favoriteRef.setValue(data);

            star = true;
        }
    }
    // --- ここまで ---//

}
