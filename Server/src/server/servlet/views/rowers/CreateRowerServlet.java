package server.servlet.views.rowers;

import com.google.gson.Gson;
import engine.api.EngineContext;
import engine.model.rower.Rower;
import engine.model.rower.RowerModifier;
import engine.utils.RegexHandler;
import server.constant.Constants;
import server.constant.ePages;
import server.servlet.json.template.ErrorsList;
import server.utils.Utils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@WebServlet(name = "CreateRower", urlPatterns = "/rowers/create")
public class CreateRowerServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Utils.renderLayout(req, resp, "/public/html/views/rowers/create.html", ePages.ROWERS);
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // TODO handle private boats
        resp.setContentType("application/json");
        String serial = req.getParameter("serialNumber");
        String name = req.getParameter("name");
        int age = Integer.parseInt(req.getParameter("age"));
        String phone = req.getParameter("phone");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        List<String> notes = splitsNotes(req.getParameter("notes"));
        boolean isAdmin = Boolean.parseBoolean(req.getParameter("isAdmin"));
        Rower.eRowerRank rank = Rower.eRowerRank.getFromInt(Integer.parseInt(req.getParameter("level")) - 1);


        try (PrintWriter out = resp.getWriter()) {
            EngineContext eng = (EngineContext) req.getServletContext().getAttribute(Constants.engineAtt);
            List<String> errors = new ArrayList<>(validatePhone(phone));

            if (!errors.isEmpty()) {
                out.println(new Gson().toJson(new ErrorsList(false, errors)));
            } else {
                Rower newRower = new Rower(serial, name, age, rank, password, isAdmin, email, phone);
                errors.addAll(validateRower(newRower, eng));

                if (!errors.isEmpty()) {
                    // Failed
                    out.println(new Gson().toJson(new ErrorsList(false, errors)));
                } else {
                    // Success
                    eng.getRowersCollectionManager().add(newRower);
                    if (notes != null) {
                        RowerModifier modifier = eng.getRowerModifier(newRower, null);
                        notes.forEach(modifier::addNewNote);
                    }
                    out.println(Utils.standardJsonResponse(true));
                }
            }
        }
    }

    private List<String> splitsNotes(String notes) {
        if (notes.length() == 0) {
            return null;
        }
        List<String> temp = Arrays.asList(notes.split(String.valueOf('\n')).clone());
        List<String> res = new ArrayList<>();
        temp.forEach(str -> res.add(str.trim()));
        return res;
    }

    private List<String> validateRower(Rower newRower, EngineContext eng) {
        List<String> result = new ArrayList<>();

        if (!eng.getRowersCollectionManager().isSerialNumberAvailable(newRower.getSerialNumber())) {
            result.add("Serial number already exist.");
        }

        if (eng.getRowersCollectionManager().emailExist(newRower.getEmail())) {
            result.add("Email address already exists.");
        }

        return result;
    }

    private List<String> validatePhone(String phone) {
        List<String> result = new ArrayList<>();
        if (!RegexHandler.isPhoneNumberValid(phone)) {
            result.add("Invalid phone number received.");
        }
        return result;
    }
}

