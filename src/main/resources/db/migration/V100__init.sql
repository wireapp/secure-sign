CREATE TABLE Events (
 messageId UUID PRIMARY KEY,
 conversationId UUID NOT NULL,
 type VARCHAR NOT NULL,
 payload VARCHAR NOT NULL,
 time TIMESTAMP NOT NULL
);

CREATE TABLE Signer (
 user_id UUID PRIMARY KEY,
 name VARCHAR NOT NULL,
 email VARCHAR NOT NULL,
 phone VARCHAR
);

CREATE TABLE SignRequest (
 request_id UUID PRIMARY KEY,
 bot_id UUID NOT NULL,
 user_id UUID NOT NULL,
 response_id UUID NOT NULL,
 document_id UUID NOT NULL
);

CREATE TABLE Document (
 document_id UUID PRIMARY KEY,
 owner UUID NOT NULL,
 name VARCHAR NOT NULL,
 original BYTEA NOT NULL,
 signed BYTEA
);

CREATE TABLE Signature (
 response_id UUID PRIMARY KEY,
 request_id UUID NOT NULL,
 document_id UUID NOT NULL,
 user_id UUID NOT NULL,
 signature BYTEA NOT NULL
);