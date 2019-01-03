package com.tictactoe.fieldservice.handler

import com.google.protobuf.util.JsonFormat
import com.tictactoe.fieldservice.handler.CommandQueueProvider.Companion.INBOUND_QUEUE_NAME
import com.tictactoe.proto.TicTacToeProto
import io.reactivex.subjects.SingleSubject
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.logging.Level
import java.util.logging.Logger


@Component
class RequestHandler {

    @Autowired
    private lateinit var sender: Sender

    fun handleCommand(cmdNewCell: TicTacToeProto.cmdNewCell) {
        val doc = TicTacToeProto.docFieldCell.newBuilder()
                .setX(cmdNewCell.x)
                .setY(cmdNewCell.y)
                .setKind(cmdNewCell.kind)
                .build()
        val respNewCell: TicTacToeProto.respNewCell = TicTacToeProto.respNewCell.newBuilder()
                .setClientRequestId(cmdNewCell.clientRequestId)
                .addCells(doc)
                .build()
        sender.send(respNewCell.toByteArray())
    }
}

@Component
class ResponseSubjectProvider {
    private var requestsMap = HashMap<Long, SingleSubject<TicTacToeProto.respNewCell>>()

    fun createAndRegister(clientRequestId: Long): SingleSubject<TicTacToeProto.respNewCell> {
        val singleSubject = SingleSubject.create<TicTacToeProto.respNewCell>()
        requestsMap[clientRequestId] = singleSubject
        return singleSubject
    }

    fun notifyAndDelete(newCellResponse: TicTacToeProto.respNewCell) {
        val clientRequestId = newCellResponse.clientRequestId
        val singleSubject = requestsMap[clientRequestId]
        singleSubject?.let {
            it.onSuccess(newCellResponse)
            requestsMap.remove(clientRequestId)
        }
    }
}

@Component
class CommandQueueProvider {

    @Value(INBOUND_QUEUE_NAME)
    lateinit var inboundQueueName: String

    @Value(OUTBOUND_QUEUE_NAME)
    lateinit var outboundQueueName: String

    @Bean
    fun commandQueue() = Queue(inboundQueueName, true)

    @Bean
    fun callBackQueue() = Queue(outboundQueueName, true)

    companion object {
        const val INBOUND_QUEUE_NAME = "#{'\${com.tictactoe.inbound-queue-name}'}"
        const val OUTBOUND_QUEUE_NAME = "#{'\${com.tictactoe.outbound-queue-name}'}"
    }
}

@Component
class Sender {

    @Autowired
    lateinit var template: RabbitTemplate

    @Autowired
    lateinit var commandQueueProvider: CommandQueueProvider

    init {
        System.out.println("Init sender")
    }

    fun send(obj: Any) {
        template.convertAndSend(commandQueueProvider.outboundQueueName, obj)
    }

    fun send(message: String) {
        template.convertAndSend(commandQueueProvider.outboundQueueName, message)
    }

    fun send(message: ByteArray) {
        template.convertAndSend(commandQueueProvider.outboundQueueName, message)
    }
}

@Service
class FieldService {
    @Bean
    fun receiver() = RabbitReceiver()
}

@RabbitListener(queues = [INBOUND_QUEUE_NAME])
class RabbitReceiver {

    @Autowired
    private lateinit var requestHandler: RequestHandler

    @RabbitHandler
    fun receive(messageIn: String) {
        Logger.getLogger("RabbitReceiver").log(Level.INFO, "[x] Received '$messageIn'")
        val commandMessageBuilder = TicTacToeProto.cmdNewCell.newBuilder()
        JsonFormat.parser().merge(messageIn, commandMessageBuilder)
        if (commandMessageBuilder.isInitialized) {
            val requestCommand = commandMessageBuilder.build()
            requestHandler.handleCommand(requestCommand)
        } else {
            Logger.getLogger("RabbitReceiver").log(Level.WARNING, "Unknown message format")
        }
    }

    @RabbitHandler
    fun receive(bytes: ByteArray) {
        Logger.getLogger("RabbitReceiver").log(Level.INFO, "[x] Received byte array")
        val commandMessage = TicTacToeProto.cmdNewCell.parseFrom(bytes)
        if (commandMessage.isInitialized) {
            requestHandler.handleCommand(commandMessage)
        } else {
            Logger.getLogger("RabbitReceiver").log(Level.WARNING, "Unknown message format")
        }
    }
}
