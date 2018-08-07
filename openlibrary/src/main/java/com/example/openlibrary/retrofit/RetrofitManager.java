package com.example.openlibrary.retrofit;

import java.io.IOException;
import java.sql.Time;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitManager {

    private static RetrofitManager mRetrofitManager;

    private final Retrofit mRetrofit;

    public RetrofitManager(String baseUrl,Map<String,String> map) {

        mRetrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(bulidOkHttpClient(map))
                .build();

    }

    public static RetrofitManager getDefault(String BASE_URL,Map<String,String> map) {

        return mRetrofitManager = new RetrofitManager(BASE_URL,map);

    }

    private OkHttpClient bulidOkHttpClient(Map<String, String> map) {

        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        PublicParamInterceptor paramInterceptor = new PublicParamInterceptor(map);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(5000, TimeUnit.MILLISECONDS)
                .writeTimeout(5000, TimeUnit.MILLISECONDS)
                .connectTimeout(5000, TimeUnit.MILLISECONDS)
                .addInterceptor(httpLoggingInterceptor)
                .addInterceptor(paramInterceptor)
                .build();

        return okHttpClient;
    }

    public <T> T create(Class<T> Clazz) {

        return mRetrofit.create(Clazz);

    }

    public class PublicParamInterceptor implements Interceptor {
        Map<String, String> paramMap = new HashMap<>();

        public PublicParamInterceptor(Map<String, String> paramMap) {
            this.paramMap = paramMap;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            //拿到原来的request
            Request oldRequest = chain.request();
            //拿到请求的url地址
            String url = oldRequest.url().toString();
            //判断是get还是post请求
            if (oldRequest.method().equalsIgnoreCase("GET")) {
                if (paramMap != null && paramMap.size() > 0) {
                    StringBuilder builder = new StringBuilder(url);
                    //接收公共的请求参数
                    for (Map.Entry<String, String> entry : paramMap.entrySet()) {
                        builder.append("&" + entry.getKey() + "=" + entry.getValue());
                    }
                    url = builder.toString();
                    //如果之前的url没有?,我们需要手动给添加一个?
                    if (!url.contains("?")) {
                        url = url.replaceFirst("&", "?");
                    }

                    //根据原来的request构造一个新的request
                    Request request = oldRequest.newBuilder()
                            .url(url)
                            .build();
                    return chain.proceed(request);
                }
            } else {
                if (paramMap != null && paramMap.size() > 0) {
                    RequestBody body = oldRequest.body();
                    if (body != null && body instanceof FormBody) {
                        FormBody formBody = (FormBody) body;
                        //把原来body里面的参数添加到新的body中
                        FormBody.Builder builder = new FormBody.Builder();
                        //为了防止添加相同的key和value
                        HashMap<String, String> temMap = new HashMap<>();
                        for (int i = 0; i < formBody.size(); i++) {
                            builder.add(formBody.encodedName(i), formBody.encodedValue(i));
                            temMap.put(formBody.encodedName(i), formBody.encodedValue(i));
                        }
                        //把公共请求的参数添加到新的body中
                        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
                            if (!temMap.containsKey(entry.getKey())) {
                                builder.add(entry.getKey(), entry.getValue());
                            }
                        }
                        FormBody newformBody = builder.build();
                        //依据原来的request构造一个新的request
                        Request request = oldRequest.newBuilder()
                                .post(newformBody)
                                .build();
                        return chain.proceed(request);
                    }
                }
            }
            return chain.proceed(oldRequest);
        }
    }
}
