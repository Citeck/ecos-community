package  ru.citeck.ecos.pages.createpages;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.JavascriptExecutor;
import ru.citeck.ecos.pages.HomePage;
import ru.citeck.ecos.pages.homepagessites.HomePageSiteContracts;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.present;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;

public class SupplementaryAgreementCreatePage extends CreatePageBase {

    public void openCreatePageSupplementaryAgreement() {
        HomePage homePage = new HomePage();
        HomePageSiteContracts homePageSiteContracts= homePage.getMenu().goToContracts();
        homePageSiteContracts.openCreateFormSupplementaryAgreement();
    }

    public void clickOnButtonMainAgreement() {
        $("[id *= \"contracts_mainAgreement-journalControl-button\"]").shouldBe(enabled).click();
    }

    public ContractCreatePage openMainContractCreatePage() {
        $("[id *= \"contracts_mainAgreement-journalControl-journalPanel-elementsTable\"] table").shouldBe(present);
        $("[id *= \"journalControl-journalPanel-elementsTable\"] table th").shouldHave(text("Created Date"));
        $("[id *= \"contracts_mainAgreement-journalControl-journalPanel-journal-picker-header\"] .create-object-button").shouldBe(enabled).shouldBe(present).click();
        ContractCreatePage contractCreatePage = new ContractCreatePage();
        return contractCreatePage;
    }

    public SelenideElement selectMainAgreement() {
        $("[id *= \"contracts_mainAgreement-journalControl-journalPanel-selectedElementsTable\"] table td").shouldBe(present);
        SelenideElement element = $("[id *= \"mainAgreement-journalControl-journalPanel-submitInput\"]");
        String js = "arguments[0].scrollIntoView();";
        ((JavascriptExecutor)getWebDriver()).executeScript(js, element);
        element.shouldBe(enabled).click();
        return $$(".value-item").get(0);
    }
}

