package ru.citeck.ecos.Tests;

import ru.citeck.ecos.pages.createpages.CreatePage;
import ru.citeck.ecos.pages.JournalsPage;
import com.codeborne.selenide.SelenideElement;
import org.junit.*;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Condition.present;


public class TestJournalPage extends SelenideTests{
    @Before
    public void openMainJournal()
    {
        JournalsPage journalsPage = new JournalsPage();
        journalsPage.openMainJournal();
    }
    @Test
    public void openCreatePageContentJournal()
    {
        JournalsPage journalsPage = new JournalsPage();
        Assert.assertTrue("Alfresco » Журналы".equals(journalsPage.getTitle()) || "Alfresco » Journals".equals(journalsPage.getTitle()));
        journalsPage.clickOnJournal(0);
        SelenideElement table = journalsPage.getTable();
        table.shouldBe(present);
        CreatePage createPage = journalsPage.clickOnButtonCreate();
        Assert.assertTrue("Alfresco » Создать контент".equals(createPage.getTitle()) || "Alfresco » Create Content".equals(createPage.getTitle()));


    }
    @Test
    public void saveFilterContentJournal()
    {
        JournalsPage journalsPage = new JournalsPage();
        journalsPage.clickOnJournal(0);
        SelenideElement table = journalsPage.clickOnFilter();
        table.shouldBe(present);
        journalsPage.addCriterionNameContainsValue();//применение фильтра
        journalsPage.getTable().$$("[tabindex = \"0\"] > tr").shouldHaveSize(4);
        journalsPage.clearFilter();
        journalsPage.getTable().$$("[tabindex = \"0\"] > tr").shouldHaveSize(10);

        journalsPage.clickOnFilter();
        table = journalsPage.clickOnFilter();
        table.shouldBe(present);
        journalsPage.addCriterionNameContainsValue();
        journalsPage.getTable().$$("[tabindex = \"0\"] > tr").shouldHaveSize(4);
        journalsPage.clickOnSaveFilter();
        journalsPage.cancelSaveFilter();

        journalsPage.clickOnSaveFilter();
        journalsPage.saveFilter();

        SelenideElement filter = journalsPage.getSavedFilter();
        Assert.assertTrue(filter.isDisplayed());
    }
    @Test
    public void saveSettingsContentJournal()
    {
        JournalsPage journalsPage = new JournalsPage();
        journalsPage.clickOnJournal(0);
        SelenideElement table = journalsPage.clickOnSettings();
        table.shouldBe(present);

        journalsPage.clickOnCheckboxSettings(4);
        journalsPage.clickOnCheckboxSettings(5);
        journalsPage.clickOnApplyButton();
        journalsPage.getTable().$$(By.cssSelector("thead th")).shouldHaveSize(6);

        journalsPage.clickClearSettings();
        journalsPage.getTable().$$(By.cssSelector("thead th")).shouldHaveSize(8);

        journalsPage.clickOnCheckboxSettings(4);
        journalsPage.clickOnSaveSettings();
        journalsPage.cancelSaveSettings();

        journalsPage.clickOnSaveSettings();
        journalsPage.setNameSettings();
        journalsPage.saveSettings();
        SelenideElement savedSetting = journalsPage.getSavedSettings();
        Assert.assertTrue(savedSetting.isDisplayed());
    }
}