package com.limo1800driver.app.domain.usecase.auth

import com.limo1800driver.app.data.model.BaseResponse
import com.limo1800driver.app.data.model.auth.VerifyOTPData
import com.limo1800driver.app.data.repository.DriverAuthRepository
import javax.inject.Inject

/**
 * Use case for verifying OTP
 */
class VerifyOTPUseCase @Inject constructor(
    private val authRepository: DriverAuthRepository
) {
    suspend operator fun invoke(
        tempUserId: String,
        otp: String
    ): Result<BaseResponse<VerifyOTPData>> {
        return authRepository.verifyOTP(
            tempUserId = tempUserId,
            otp = otp
        )
    }
}

