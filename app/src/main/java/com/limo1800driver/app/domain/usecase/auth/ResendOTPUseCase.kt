package com.limo1800driver.app.domain.usecase.auth

import com.limo1800driver.app.data.model.BaseResponse
import com.limo1800driver.app.data.model.auth.AuthData
import com.limo1800driver.app.data.repository.DriverAuthRepository
import javax.inject.Inject

/**
 * Use case for resending OTP
 */
class ResendOTPUseCase @Inject constructor(
    private val authRepository: DriverAuthRepository
) {
    suspend operator fun invoke(
        tempUserId: String
    ): Result<BaseResponse<AuthData>> {
        return authRepository.resendOTP(tempUserId)
    }
}

