package auth.controller;

import auth.model.RegistrationModel;
import auth.service.RabbitMqService;
import auth.service.UserService;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegisterController {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterController.class);

    private static final String REGISTRATION_VIEW_NAME = "register",
            MODEL_USERNAME_ATTR = "username",
            MODEL_REG_FORM_ATTR = "registrationModel",
            MODEL_FRONT_URI_ATTR = "frontUri",
            REG_MODEL_ERROR_BEAN_NAME = "org.springframework.validation.BindingResult.registrationModel";

    private final UserService userService;
    private final RabbitMqService rabbitMqService;
    private final String frontUri;

    @Autowired
    public RegisterController(UserService userService,
                              RabbitMqService rabbitMqService,
                              @Value("${rangiffler-front.base-uri}") String frontUri) {
        this.userService = userService;
        this.rabbitMqService = rabbitMqService;
        this.frontUri = frontUri;
    }

    @GetMapping("/register")
    public String getRegisterPage(Model model) {
        model.addAttribute(MODEL_REG_FORM_ATTR, new RegistrationModel());
        model.addAttribute(MODEL_FRONT_URI_ATTR, frontUri + "/redirect");
        return REGISTRATION_VIEW_NAME;
    }

    @PostMapping(value = "/register")
    public String registerUser(@Valid @ModelAttribute RegistrationModel registrationModel,
                               Errors errors,
                               Model model,
                               HttpServletResponse response) {
        if (!errors.hasErrors()) {
            final String registeredUserName;
            try {
                registeredUserName = userService.registerUser(
                        registrationModel.getUsername(),
                        registrationModel.getPassword()
                );
                rabbitMqService.sendRegisteredUsername(registeredUserName);
                response.setStatus(HttpServletResponse.SC_CREATED);
                model.addAttribute(MODEL_USERNAME_ATTR, registeredUserName);
            } catch (DataIntegrityViolationException e) {
                LOG.error("### Error while registration user: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                addErrorToRegistrationModel(
                        registrationModel,
                        model,
                        "username", "Username `" + registrationModel.getUsername() + "` already exists"
                );
            }
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        model.addAttribute(MODEL_FRONT_URI_ATTR, frontUri + "/redirect");
        return REGISTRATION_VIEW_NAME;
    }

    private void addErrorToRegistrationModel(@Nonnull RegistrationModel registrationModel,
                                             @Nonnull Model model,
                                             @Nonnull String fieldName,
                                             @Nonnull String error) {
        BeanPropertyBindingResult errorResult = (BeanPropertyBindingResult) model.getAttribute(REG_MODEL_ERROR_BEAN_NAME);
        if (errorResult == null) {
            errorResult = new BeanPropertyBindingResult(registrationModel, "registrationModel");
        }
        errorResult.addError(new FieldError("registrationModel", fieldName, error));
    }

}
