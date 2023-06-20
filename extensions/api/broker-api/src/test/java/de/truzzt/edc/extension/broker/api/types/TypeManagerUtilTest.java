package de.truzzt.edc.extension.broker.api.types;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class TypeManagerUtilTest {

    private final String CONNECTOR_UPDATE_MESSAGE_WITH_TOKEN =
            """
            {
              "@context" : {
                "ids" : "https://w3id.org/idsa/core/",
                "idsc" : "https://w3id.org/idsa/code/"
              },
              "@type" : "ids:ConnectorUpdateMessage",
              "@id" : "https://w3id.org/idsa/autogen/connectorUpdateMessage/6d875403-cfea-4aad-979c-3515c2e71967",
              "ids:securityToken" : {
                "@type" : "ids:DynamicAttributeToken",
                "@id" : "https://w3id.org/idsa/autogen/dynamicAttributeToken/7bbbd2c1-2d75-4e3d-bd10-c52d0381cab0",
                "ids:tokenValue" : "eyJ0eXAiOiJKV1QiLCJraWQiOiJkZWZhdWx0IiwiYWxnIjoiUlMyNTYifQ.eyJzY29wZXMiOlsiaWRzYzpJRFNfQ09OTkVDVE9SX0FUVFJJQlVURVNfQUxMIl0sImF1ZCI6Imlkc2M6SURTX0NPTk5FQ1RPUlNfQUxMIiwiaXNzIjoiaHR0cHM6Ly9kYXBzLmFpc2VjLmZyYXVuaG9mZXIuZGUiLCJuYmYiOjE2MzQ2NTA3MzksImlhdCI6MTYzNDY1MDczOSwianRpIjoiTVRneE9EUXdPVFF6TXpZd05qWXlOVFExTUE9PSIsImV4cCI6MTYzNDY1NDMzOSwic2VjdXJpdHlQcm9maWxlIjoiaWRzYzpCQVNFX1NFQ1VSSVRZX1BST0ZJTEUiLCJyZWZlcnJpbmdDb25uZWN0b3IiOiJodHRwOi8vYnJva2VyLmlkcy5pc3N0LmZyYXVuaG9mZXIuZGUuZGVtbyIsIkB0eXBlIjoiaWRzOkRhdFBheWxvYWQiLCJAY29udGV4dCI6Imh0dHBzOi8vdzNpZC5vcmcvaWRzYS9jb250ZXh0cy9jb250ZXh0Lmpzb25sZCIsInRyYW5zcG9ydENlcnRzU2hhMjU2IjoiOTc0ZTYzMjRmMTJmMTA5MTZmNDZiZmRlYjE4YjhkZDZkYTc4Y2M2YTZhMDU2NjAzMWZhNWYxYTM5ZWM4ZTYwMCIsInN1YiI6IjkyOjE0OkU3OkFDOjEwOjIyOkYyOkNDOjA1OjZFOjJBOjJCOjhEOkRCOjEwOkQ2OjREOkEwOkExOjUzOmtleWlkOkNCOjhDOkM3OkI2Ojg1Ojc5OkE4OjIzOkE2OkNCOjE1OkFCOjE3OjUwOjJGOkU2OjY1OjQzOjVEOkU4In0.Qw3gWMgwnKQyVatbsozcin6qtQbLyXlk6QdaLajGaDmxSYqCKEcAje4kiDp5Fqj04WPmVyF0k8c1BJA3KGnaW3Qcikv4MNxqqoenvKIrSTokXsA7-osqBCfxLhV-s2lSXVTAtV_Q7f71eSoR5j-7nPPX8_nf4Xup4_VzfnwRmnuAbLfHfWThbupxFazC34r3waXCltOTFVa_XDlwEDMpPY7vEPeaqIt2t6ofVGo_HF86UB19liL-UZvp0uSE9z2fhloyxOrx9B_xavGS7pP6oRaumSJEN_x9dfdeDS98HQ_oBSSGBzaI4fM7ik35Yg42KQwmkZesD6P_YSEzVLcJDg",
                "ids:tokenFormat" : {
                    "@id" : "idsc:JWT"
                }
              },
              "ids:senderAgent" : {
                "@id" : "http://example.org"
              },
              "ids:modelVersion" : "4.0.0",
              "ids:issuerConnector" : {
                "@id" : "https://test.connector.de/testDataModel"
              },
              "ids:issued" : {
                "@value" : "2021-06-23T17:27:23.566+02:00",
                "@type" : "http://www.w3.org/2001/XMLSchema#dateTimeStamp"
              },
              "ids:affectedConnector" : {
                "@id" : "https://test.connector.de/testDataModel"
              }
            }""";

    private final String CONNECTOR_UPDATE_MESSAGE_WITHOUT_TOKEN =
            """
            {
              "@context" : {
                "ids" : "https://w3id.org/idsa/core/",
                "idsc" : "https://w3id.org/idsa/code/"
              },
              "@type" : "ids:ConnectorUpdateMessage",
              "@id" : "https://w3id.org/idsa/autogen/connectorUpdateMessage/6d875403-cfea-4aad-979c-3515c2e71967",
              "ids:senderAgent" : {
                "@id" : "http://example.org"
              },
              "ids:modelVersion" : "4.0.0",
              "ids:issuerConnector" : {
                "@id" : "https://test.connector.de/testDataModel"
              },
              "ids:issued" : {
                "@value" : "2021-06-23T17:27:23.566+02:00",
                "@type" : "http://www.w3.org/2001/XMLSchema#dateTimeStamp"
              },
              "ids:affectedConnector" : {
                "@id" : "https://test.connector.de/testDataModel"
              }
            }""";

    private TypeManagerUtil typeManagerUtil;

    @BeforeEach
    void setUp() {
        typeManagerUtil = new TypeManagerUtil();
    }

    @Test
    void parseMessageWithToken() {
        InputStream headerInputStream = new ByteArrayInputStream(CONNECTOR_UPDATE_MESSAGE_WITH_TOKEN.getBytes());

        var header = typeManagerUtil.parseMessage(headerInputStream);
        Assertions.assertNotNull(header);

        var jwt = typeManagerUtil.parseToken(header.getSecurityToken());
        Assertions.assertNotNull(jwt);
    }

    @Test
    void parseMessageWithoutToken() {
        InputStream headerInputStream = new ByteArrayInputStream(CONNECTOR_UPDATE_MESSAGE_WITHOUT_TOKEN.getBytes());

        var header = typeManagerUtil.parseMessage(headerInputStream);
        Assertions.assertNotNull(header);
    }
}
