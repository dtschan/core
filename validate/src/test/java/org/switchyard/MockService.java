package org.switchyard;

import javax.xml.namespace.QName;

import org.switchyard.metadata.ServiceInterface;

public class MockService implements Service {

    private QName _name;

    public MockService(String name) {
        _name = QName.valueOf(name);
    }

    @Override
    public QName getName() {
        return _name;
    }

    @Override
    public ServiceInterface getInterface() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void unregister() {
        // TODO Auto-generated method stub

    }

    @Override
    public ServiceDomain getDomain() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExchangeHandler getProviderHandler() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceMetadata getServiceMetadata() {
        // TODO Auto-generated method stub
        return null;
    }

}
