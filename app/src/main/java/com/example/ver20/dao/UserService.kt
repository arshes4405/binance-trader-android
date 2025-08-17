package com.example.ver20.dao

import android.content.Context
import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.security.MessageDigest

// 테스트용 API 인터페이스
interface TestApi {
    @GET("api")
    fun testAction(@Query("action") action: String): Call<UserApiResponse>
}

// Vercel API 인터페이스 (계정 관리용)
interface UserApi {
    @GET("api/user")
    fun getUser(@Query("action") action: String = "getUser", @Query("username") username: String): Call<UserApiResponse>

    @POST("api/user")
    fun createUser(
        @Query("action") action: String = "createUser",
        @Body request: CreateUserRequest
    ): Call<UserApiResponse>

    @POST("api/user")
    fun loginUser(
        @Query("action") action: String = "loginUser",
        @Body request: LoginUserRequest
    ): Call<UserApiResponse>
}

// 요청/응답 데이터 클래스
data class UserApiResponse(
    val success: Boolean,
    val data: UserData? = null,
    val message: String? = null
)

data class UserData(
    val _id: String,
    val username: String,
    val email: String,
    val createdAt: String
)

data class CreateUserRequest(
    val username: String,
    val email: String,
    val password: String
)

data class LoginUserRequest(
    val username: String,
    val password: String
)

class UserService {
    private val baseUrl = "https://binance-trader-api.vercel.app/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(UserApi::class.java)

    // 비밀번호 해시화 함수
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // 서버 API 구조 디버깅
    fun debugApiStructure(callback: (String) -> Unit) {
        Log.d("UserService", "API 구조 디버깅 시작")

        val results = mutableListOf<String>()

        // 1. 기본 GET 요청 (쿼리 파라미터 없이)
        retrofit.create(TestApi::class.java).testAction("").enqueue(object : Callback<UserApiResponse> {
            override fun onResponse(call: Call<UserApiResponse>, response: Response<UserApiResponse>) {
                val errorBody = response.errorBody()?.string()
                results.add("GET /api (파라미터 없음): ${response.code()}")
                if (errorBody != null) {
                    results.add("응답: $errorBody")
                }

                // 2. POST 요청으로 테스트
                val testRequest = CreateUserRequest("test", "test@test.com", "test123")

                api.createUser(request = testRequest).enqueue(object : Callback<UserApiResponse> {
                    override fun onResponse(call: Call<UserApiResponse>, response: Response<UserApiResponse>) {
                        val postErrorBody = response.errorBody()?.string()
                        results.add("POST /api/user?action=createUser: ${response.code()}")
                        if (postErrorBody != null) {
                            results.add("POST 응답: $postErrorBody")
                        }

                        callback(results.joinToString("\n"))
                    }

                    override fun onFailure(call: Call<UserApiResponse>, t: Throwable) {
                        results.add("POST 실패: ${t.message}")
                        callback(results.joinToString("\n"))
                    }
                })
            }

            override fun onFailure(call: Call<UserApiResponse>, t: Throwable) {
                results.add("GET 실패: ${t.message}")
                callback(results.joinToString("\n"))
            }
        })
    }

    // 디버깅을 포함한 계정 생성
    fun createAccountWithDebug(
        username: String,
        email: String,
        password: String,
        callback: (Boolean, String?) -> Unit
    ) {
        Log.d("UserService", "디버그 모드로 계정 생성 시작")

        // 먼저 API 구조 디버깅
        debugApiStructure { debugResults ->
            Log.d("UserService", "API 디버깅 결과:\n$debugResults")

            // 실제 계정 생성 시도
            try {
                val hashedPassword = hashPassword(password)

                // MongoDbService를 사용한 계정 생성 시도
                val mongoService = MongoDbService()
                mongoService.saveUserSettings(username, email, hashedPassword) { success, message ->
                    if (success) {
                        Log.d("UserService", "MongoDB를 통한 계정 생성 성공")
                        callback(true, "계정이 성공적으로 생성되었습니다")
                    } else {
                        Log.e("UserService", "MongoDB 계정 생성 실패: $message")
                        callback(false, """
                            계정 생성 실패: $message
                            
                            API 디버깅 정보:
                            $debugResults
                            
                            서버 API 구조를 확인해주세요.
                        """.trimIndent())
                    }
                }
            } catch (e: Exception) {
                Log.e("UserService", "계정 생성 중 예외: ${e.message}")
                callback(false, "계정 생성 중 오류: ${e.message}")
            }
        }
    }

    // 계정 생성 (기존 방식)
    fun createAccount(
        username: String,
        email: String,
        password: String,
        callback: (Boolean, String?) -> Unit
    ) {
        try {
            Log.d("UserService", "사용자 설정 저장 시작: username=$username, email=$email")

            // 비밀번호 해시화
            val hashedPassword = hashPassword(password)

            // MongoDbService를 활용해서 user_settings 컬렉션에 저장
            val mongoService = MongoDbService()
            mongoService.saveUserSettings(username, email, hashedPassword) { success, message ->
                if (success) {
                    Log.d("UserService", "사용자 설정 저장 성공: $username")
                    callback(true, "계정이 성공적으로 생성되었습니다")
                } else {
                    Log.e("UserService", "사용자 설정 저장 실패: $message")
                    callback(false, message ?: "계정 생성에 실패했습니다")
                }
            }

        } catch (e: Exception) {
            Log.e("UserService", "계정 생성 중 예외 발생: ${e.message}", e)
            callback(false, "계정 생성 중 오류가 발생했습니다: ${e.message}")
        }
    }

    // UserData 생성을 위한 확장 함수
    fun Map<String, Any>.toUserData(fallbackUsername: String): UserData? {
        return try {
            val id = when (val idValue = this["_id"]) {
                is String -> idValue
                else -> "user_${System.currentTimeMillis()}"
            }

            val username = when (val usernameValue = this["username"]) {
                is String -> usernameValue
                else -> fallbackUsername
            }

            val email = when (val emailValue = this["email"]) {
                is String -> emailValue
                else -> ""
            }

            val createdAt = when (val createdAtValue = this["createdAt"]) {
                is String -> createdAtValue
                is Number -> createdAtValue.toString()
                else -> System.currentTimeMillis().toString()
            }

            UserData(_id = id, username = username, email = email, createdAt = createdAt)
        } catch (e: Exception) {
            Log.e("UserService", "UserData 변환 실패: ${e.message}")
            null
        }
    }

    // 개선된 로그인 함수
    fun loginUser(
        username: String,
        password: String,
        callback: (Boolean, UserData?, String?) -> Unit
    ) {
        try {
            Log.d("UserService", "사용자 로그인 시도: $username")

            val hashedPassword = hashPassword(password)
            Log.d("UserService", "비밀번호 해시: ${hashedPassword.take(10)}...")

            val mongoService = MongoDbService()
            mongoService.getUserSettings(username) { success, userData, error ->
                if (success && userData != null) {
                    try {
                        // userData는 ApiResponse 구조이므로 data 필드에서 실제 데이터 추출
                        val actualUserData = when (val dataField = userData["data"]) {
                            is Map<*, *> -> {
                                @Suppress("UNCHECKED_CAST")
                                dataField as Map<String, Any>
                            }
                            else -> {
                                Log.e("UserService", "data 필드가 Map이 아님: ${dataField?.javaClass}")
                                callback(false, null, "사용자 데이터 구조 오류")
                                return@getUserSettings
                            }
                        }

                        // 이제 actualUserData에서 password 접근
                        val storedPassword = when (val passwordValue = actualUserData["password"]) {
                            is String -> passwordValue
                            else -> {
                                Log.e("UserService", "저장된 비밀번호가 String이 아님: ${passwordValue?.javaClass}")
                                callback(false, null, "사용자 데이터 형식 오류")
                                return@getUserSettings
                            }
                        }

                        Log.d("UserService", "저장된 비밀번호 해시: ${storedPassword.take(10)}...")

                        if (storedPassword == hashedPassword) {
                            // 로그인 성공 - actualUserData에서 UserData 생성
                            val userDataResult = actualUserData.toUserData(username)

                            if (userDataResult != null) {
                                Log.d("UserService", "✅ 로그인 성공: $username")
                                callback(true, userDataResult, "로그인 성공")
                            } else {
                                Log.e("UserService", "❌ UserData 생성 실패")
                                callback(false, null, "사용자 데이터 변환 실패")
                            }
                        } else {
                            Log.e("UserService", "❌ 비밀번호 불일치")
                            callback(false, null, "비밀번호가 일치하지 않습니다")
                        }
                    } catch (e: Exception) {
                        Log.e("UserService", "❌ 로그인 처리 중 예외: ${e.message}", e)
                        callback(false, null, "로그인 처리 중 오류: ${e.message}")
                    }
                } else {
                    Log.e("UserService", "❌ 사용자 조회 실패: $error")
                    callback(false, null, error ?: "사용자를 찾을 수 없습니다")
                }
            }

        } catch (e: Exception) {
            Log.e("UserService", "❌ 로그인 중 예외 발생: ${e.message}", e)
            callback(false, null, "로그인 중 오류가 발생했습니다: ${e.message}")
        }
    }

    // 사용자 정보 조회
    fun getUser(username: String, callback: (Boolean, UserData?, String?) -> Unit) {
        api.getUser(username = username).enqueue(object : Callback<UserApiResponse> {
            override fun onResponse(call: Call<UserApiResponse>, response: Response<UserApiResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    callback(true, response.body()?.data, null)
                } else {
                    val error = response.body()?.message ?: "사용자 조회 실패"
                    callback(false, null, error)
                }
            }

            override fun onFailure(call: Call<UserApiResponse>, t: Throwable) {
                callback(false, null, "네트워크 오류: ${t.message}")
            }
        })
    }

    // SharedPreferences에 사용자 정보 저장
    fun saveUserToPreferences(context: Context, userData: UserData) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("user_logged_in", true)
            putString("user_id", userData._id)
            putString("username", userData.username)
            putString("email", userData.email)
            putString("created_at", userData.createdAt)
            apply()
        }
        Log.d("UserService", "사용자 정보 저장됨: ${userData.username}")
    }

    // SharedPreferences에서 사용자 정보 조회
    fun getUserFromPreferences(context: Context): UserData? {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("user_logged_in", false)

        return if (isLoggedIn) {
            UserData(
                _id = prefs.getString("user_id", "") ?: "",
                username = prefs.getString("username", "") ?: "",
                email = prefs.getString("email", "") ?: "",
                createdAt = prefs.getString("created_at", "") ?: ""
            )
        } else {
            null
        }
    }

    // 로그아웃 (SharedPreferences 클리어)
    fun logout(context: Context) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        Log.d("UserService", "로그아웃 완료")
    }

    // 서버 연결 테스트 및 지원하는 액션 확인
    fun testServerConnection(callback: (Boolean, String) -> Unit) {
        try {
            Log.d("UserService", "서버 연결 테스트 시작: $baseUrl")

            // 1. 기본 연결 테스트 (GET 요청)
            api.getUser(username = "test").enqueue(object : Callback<UserApiResponse> {
                override fun onResponse(call: Call<UserApiResponse>, response: Response<UserApiResponse>) {
                    val isConnected = response.code() in 200..499
                    Log.d("UserService", "GET 테스트 - 응답 코드: ${response.code()}")

                    if (response.errorBody() != null) {
                        val errorBody = response.errorBody()?.string()
                        Log.d("UserService", "GET 오류 바디: $errorBody")
                    }

                    callback(isConnected, "서버 연결 ${if (isConnected) "성공" else "실패"} (코드: ${response.code()})")
                }

                override fun onFailure(call: Call<UserApiResponse>, t: Throwable) {
                    Log.e("UserService", "서버 연결 실패: ${t.message}")
                    callback(false, "서버 연결 실패: ${t.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("UserService", "연결 테스트 중 오류: ${e.message}")
            callback(false, "연결 테스트 오류: ${e.message}")
        }
    }

    // API 액션 테스트
    fun testApiActions(callback: (String) -> Unit) {
        Log.d("UserService", "API 액션 테스트 시작")

        // 다양한 액션으로 테스트
        val testActions = listOf("test", "ping", "status", "getFavoriteCoins", "createUser")
        val results = mutableListOf<String>()
        var completedTests = 0

        testActions.forEach { action ->
            // GET 요청으로 액션 테스트
            retrofit.create(TestApi::class.java).testAction(action).enqueue(object : Callback<UserApiResponse> {
                override fun onResponse(call: Call<UserApiResponse>, response: Response<UserApiResponse>) {
                    val result = "$action: ${response.code()} - ${response.errorBody()?.string() ?: "OK"}"
                    results.add(result)
                    Log.d("UserService", "액션 테스트 - $result")

                    completedTests++
                    if (completedTests == testActions.size) {
                        callback(results.joinToString("\n"))
                    }
                }

                override fun onFailure(call: Call<UserApiResponse>, t: Throwable) {
                    results.add("$action: FAIL - ${t.message}")
                    completedTests++
                    if (completedTests == testActions.size) {
                        callback(results.joinToString("\n"))
                    }
                }
            })
        }
    }
}