package ru.citeck.ecos.Tests;

import ru.citeck.ecos.pages.AdminToolsPage;
import ru.citeck.ecos.pages.HomePage;
import ru.citeck.ecos.pages.LoginPage;
import com.codeborne.selenide.SelenideElement;
import org.junit.*;

import static com.codeborne.selenide.Condition.present;

public class TestAdminTools extends SelenideTests{
    @Before
    public void openAdminTools()
    {
        HomePage homePage = new HomePage();
        homePage.openAdminTools();
    }
    @Test
    public void createNewUser()
    {
        String username="userName";
        String login="user"+Math.random();
        String password="Qq123456";

        AdminToolsPage adminToolsPage =  new AdminToolsPage();
        SelenideElement userPanel = adminToolsPage.openUserContent();
        userPanel.shouldBe(present);

        SelenideElement userFormCreate = adminToolsPage.clickOnButtonNewUser();
        userFormCreate.shouldBe(present);

        adminToolsPage.setValueOnFromCreateNewUser(username,login,password);
        adminToolsPage.clickOnButtonCreate();

        SelenideElement results = adminToolsPage.searchUser(login);
        results.shouldBe(present);

        LoginPage loginPage = adminToolsPage.logOut();
        loginPage.inLoginAndPassword(login,password);
        HomePage homePage = loginPage.pressEnter();
        Assert.assertTrue("Alfresco » Домашняя страница".equals(homePage.getTitle()) || "Alfresco » User Dashboard".equals(homePage.getTitle()));
    }
}