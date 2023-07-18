# BlockStoreDemo
Android Block Store Demo

#How to test
Same device uninstall/reinstall
If the user enables Backup services (it can be checked at Settings > Google > Backup), Block Store data is persisted across the app uninstall/reinstall.

You can follow these steps to test:

1.Integrate the BlockStore API to your test app.
2.Use the test app to invoke the BlockStore API to store your data.
3.Uninstall your test app and then reinstall your app on the same device.
4.Use the test app to invoke the BlockStore API to retrieve your data.
5.Verify that the bytes retrieved are the same as what were stored before uninstallation.
