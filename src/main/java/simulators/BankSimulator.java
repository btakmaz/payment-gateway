package simulators;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.springframework.stereotype.Component;

@Component
public class BankSimulator {
  public String processPayment(PostPaymentRequest payment) {
    String cardNumber = String.valueOf(payment.getCardNumberLastFour());
    char lastDigit = cardNumber.charAt(cardNumber.length() - 1);

    if (lastDigit == '0') {
      throw new RuntimeException("503 Service Unavailable");
    }

    if (lastDigit % 2 == 0) {
      return "Declined";
    } else {
      return "Authorized";
    }
  }
}
