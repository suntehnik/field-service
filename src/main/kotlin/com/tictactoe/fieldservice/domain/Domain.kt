package com.tictactoe.fieldservice.domain

import com.tictactoe.proto.TicTacToeProto
import org.hibernate.annotations.GenericGenerator
import javax.persistence.*

@Entity
@Table(uniqueConstraints =
[UniqueConstraint(columnNames = ["x", "y"])]
)

class CellStore(@Id
                @GeneratedValue(generator = "system-uuid")
                @GenericGenerator(name = "system-uuid", strategy = "uuid")
                val id: String,
                @Embedded
                val cell: Cell)

@Embeddable
class Cell(val kind: CellKind, val x: Long, val y: Long)

enum class CellKind(val kind: Int) {
    X(0), O(1);

    companion object {

        fun valueOf(value: TicTacToeProto.Kind): CellKind {
            return when (value) {
                TicTacToeProto.Kind.O -> O
                TicTacToeProto.Kind.X -> X
                else -> throw IllegalArgumentException("Unsupported cell kind")
            }
        }
    }
}
