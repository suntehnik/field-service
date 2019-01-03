package com.tictactoe.fieldservice.usecase

import com.tictactoe.fieldservice.dao.CellRepository
import com.tictactoe.fieldservice.domain.Cell
import com.tictactoe.fieldservice.domain.CellKind
import com.tictactoe.fieldservice.domain.CellStore
import com.tictactoe.proto.TicTacToeProto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class WriteToRepository {
    @Autowired
    private lateinit var cellRepository: CellRepository

    fun writeToRepository(kind: TicTacToeProto.Kind, x: Long, y: Long): CellStore {
        val cell = Cell(CellKind.valueOf(kind), x, y)
        val cellStore = CellStore("", cell)
        return cellRepository.save(cellStore)
    }
}