package com.bytedance.network;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bytedance.network.api.GitHubService;
import com.bytedance.network.model.Repo;
import com.bytedance.network.socket.SocketTestActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MainActivity extends AppCompatActivity {
    public static final String TAG = "network_demo";

    private final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private int page = 0;
    private String content = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_base).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestBase("JakeWharton");
            }
        });

        findViewById(R.id.btn_retrofit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //requestRetrofitSync("JakeWharton");
                requestRetrofitAsync("JakeWharton");
            }
        });

        findViewById(R.id.clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((TextView) findViewById(R.id.tv)).setText("");
                page = 0;
            }
        });
        findViewById(R.id.socket).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SocketTestActivity.class));
            }
        });

//        GitHubService service = new GitHubService() {
//            @Override
//            public Call<List<Repo>> getRepos(String userName, int page, int perPage, String accept) {
//                //????????????????????????????????????
//
//                Call<Response> call = new Call() {
//                    @Override
//                    public Response execute() throws IOException {
//                        // ????????????????????????????????????????????????http????????????????????????GsonConvert??????List<Repo>??????
//                        return response;
//                    }
//                };
//                return call;
//            }
//        };

    }

    /**
     * ??????UI
     *
     * @param repoList
     */
    private void showRepos(List<Repo> repoList) {
        Log.d(TAG, "repo list add " + repoList.size());
        StringBuilder stringBuilder = new StringBuilder(content);
        for (int i = 0; i < repoList.size(); i++) {
            final Repo repo = repoList.get(i);
            stringBuilder.append("????????????").append(repo.getName())
                    .append("\n fork ?????????").append(repo.getForksCount())
                    .append("\n star ?????????").append(repo.getStarsCount())
                    .append("\n\n");
        }
        content = stringBuilder.toString();

        ((TextView) findViewById(R.id.tv)).setText(content);
    }



    /**
     * ???????????????URLConnection??????????????????
     *
     * @param userName
     */
    private void requestBase(final String userName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Repo> repos = baseGetReposFromRemote(
                        userName, page, 10, "application/vnd.github.v3+json");
                if (repos != null && !repos.isEmpty()) {
                    page++;
                    new Handler(getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            showRepos(repos);
                        }
                    });

                }
            }
        }).start();
    }



    public List<Repo> baseGetReposFromRemote(String userName, int page, int perPage, String accept) {
        String urlStr =
                String.format("https://api.github.com/users/%s/repos?page=%d&per_page=%d", userName, page, perPage);
        List<Repo> result = null;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(6000);

            conn.setRequestMethod("GET");

            conn.setRequestProperty("accept", accept);

            if (conn.getResponseCode() == 200) {

                InputStream in = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

                result = new Gson().fromJson(reader, new TypeToken<List<Repo>>() {
                }.getType());

                reader.close();
                in.close();

            } else {
                // ????????????
            }
            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "????????????" + e.toString(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        return result;
    }

    /**
     * Retrofit ??????????????????
     *
     * @param userName
     */
    private void requestRetrofitSync(final String userName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                GitHubService service = retrofit.create(GitHubService.class);
                Call<List<Repo>> call = service.getRepos(userName, page, 10, "application/vnd.github.v3+json");
                try {
                    Response<List<Repo>> response = call.execute();
                    if (response.isSuccessful() && !response.body().isEmpty()) {
                        page++;
                        new Handler(getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                showRepos(response.body());
                            }
                        });

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }


    /**
     * Retrofit??????????????????
     *
     * @param userName
     */
    private void requestRetrofitAsync(String userName) {

        GitHubService service = retrofit.create(GitHubService.class);

        Call<List<Repo>> repos = service.getRepos(userName, page, 10, "application/vnd.github.v3+json");
        repos.enqueue(new Callback<List<Repo>>() {
            @Override
            public void onResponse(final Call<List<Repo>> call, final Response<List<Repo>> response) {
                if (!response.isSuccessful()) {
                    return;
                }
                final List<Repo> repoList = response.body();
                if (repoList == null || repoList.isEmpty()) {
                    return;
                }
                page++;
                showRepos(repoList);
            }

            @Override
            public void onFailure(final Call<List<Repo>> call, final Throwable t) {
                t.printStackTrace();
            }
        });
    }
}