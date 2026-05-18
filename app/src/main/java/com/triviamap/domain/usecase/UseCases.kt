package com.triviamap.domain.usecase

import com.triviamap.domain.model.GameResult
import com.triviamap.domain.model.TramLine
import com.triviamap.domain.repository.GameResultRepository
import com.triviamap.domain.repository.TramLineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllLinesUseCase @Inject constructor(
    private val repo: TramLineRepository
) {
    operator fun invoke(): Flow<List<TramLine>> = repo.getAllLines()
}

class GetLineUseCase @Inject constructor(
    private val repo: TramLineRepository
) {
    suspend operator fun invoke(id: String): TramLine? = repo.getLine(id)
}

class SaveGameResultUseCase @Inject constructor(
    private val repo: GameResultRepository
) {
    suspend operator fun invoke(result: GameResult) = repo.saveResult(result)
}

class GetBestScoresUseCase @Inject constructor(
    private val repo: GameResultRepository
) {
    operator fun invoke(): Flow<List<GameResult>> = repo.getBestScores()
}

class GetLineResultsUseCase @Inject constructor(
    private val repo: GameResultRepository
) {
    operator fun invoke(lineId: String): Flow<List<GameResult>> =
        repo.getResultsForLine(lineId)
}
