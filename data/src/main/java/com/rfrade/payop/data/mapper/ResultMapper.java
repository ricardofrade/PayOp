package com.rfrade.payop.data.mapper;

import com.elecctro.recruitment.paymentterminal.AuthorizationResult;
import com.elecctro.recruitment.paymentterminal.State;
import com.elecctro.recruitment.paymentterminal.TerminalResult;
import com.rfrade.payop.domain.model.AuthResult;
import com.rfrade.payop.domain.model.OperationResult;

public final class ResultMapper {

    private ResultMapper() {
    }

    public static AuthResult toAuthResult(AuthorizationResult sdk) {
        AuthResult.Outcome outcome;
        if (sdk.state == State.APPROVED) {
            outcome = AuthResult.Outcome.APPROVED;
        } else if (sdk.state == State.TIMED_OUT) {
            outcome = AuthResult.Outcome.TIMED_OUT;
        } else {
            outcome = AuthResult.Outcome.DECLINED;
        }

        String errorCode = sdk.error != null ? sdk.error.name() : null;
        return new AuthResult(outcome, sdk.approvedAmount, errorCode);
    }

    public static OperationResult toOperationResult(TerminalResult sdk) {
        OperationResult.Outcome outcome;
        if (sdk.state == State.APPROVED) {
            outcome = OperationResult.Outcome.APPROVED;
        } else if (sdk.state == State.TIMED_OUT) {
            outcome = OperationResult.Outcome.TIMED_OUT;
        } else {
            outcome = OperationResult.Outcome.DECLINED;
        }

        String errorCode = sdk.error != null ? sdk.error.name() : null;
        return new OperationResult(outcome, errorCode);
    }
}
