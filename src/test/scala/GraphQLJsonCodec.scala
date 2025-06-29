import caliban.InputValue.*
import caliban.Value.*
import caliban.{GraphQLRequest, InputValue}
import zio.json.*
import zio.json.ast.Json
import zio.prelude.ForEach.*

import scala.collection.immutable.List
import scala.math.BigDecimal.javaBigDecimal2bigDecimal
import scala.util.Either

object GraphQLJsonCodec {

  // ENCODERS from the previous request for completeness

  implicit val inputValueEncoder: JsonEncoder[InputValue] = Json.encoder.contramap[InputValue] {
    case StringValue(value) => Json.Str(value)
    case BooleanValue(value) => Json.Bool(value)
    case EnumValue(value) => Json.Str(value)
    case IntValue.IntNumber(value) => Json.Num(value)
    case IntValue.LongNumber(value) => Json.Num(value)
    case IntValue.BigIntNumber(value) => Json.Num(value)
    case FloatValue.FloatNumber(value) => Json.Num(value)
    case FloatValue.DoubleNumber(value) => Json.Num(value)
    case FloatValue.BigDecimalNumber(value) => Json.Num(value)
    case NullValue => Json.Null
    case ListValue(values) => Json.Arr(values.map(v => Json.decoder.decodeJson(v.toJson).toOption.get)*)
    case ObjectValue(fields) => Json.Obj(fields.map { case (k, v) => k -> Json.decoder.decodeJson(v.toJson).toOption.get }.toSeq*)
    case VariableValue(name) => Json.Str(s"$$$name")
  }

  implicit val graphQLRequestEncoder: JsonEncoder[GraphQLRequest] = Json.encoder.contramap[GraphQLRequest] { request =>
    val fields = List.newBuilder[(String, Json)]
    
    request.query.foreach { query =>
      fields += "query" -> Json.Str(query)
    }
    
    request.operationName.foreach { operationName =>
      fields += "operationName" -> Json.Str(operationName)
    }
    
    request.variables.foreach { variables =>
      val variableFields = variables.map { case (k, v) => 
        k -> Json.decoder.decodeJson(v.toJson).toOption.get 
      }.toSeq
      fields += "variables" -> Json.Obj(variableFields*)
    }
    
    Json.Obj(fields.result()*)
  }

}