
    create table ArtificerArtifact (
        id bigint generated by default as identity,
        content blob,
        contentEncoding varchar(255),
        contentHash char(40),
        contentPath varchar(255),
        contentSize bigint not null,
        createdTime timestamp,
        createdByUsername varchar(50),
        derived boolean not null,
        description clob,
        mimeType varchar(100),
        model varchar(255),
        modifiedTime timestamp,
        modifiedByUsername varchar(50),
        name varchar(255),
        trashed boolean not null,
        type varchar(255),
        uuid char(36),
        version varchar(255),
        derivedFrom_id bigint,
        primary key (id)
    );

    create table ArtificerArtifact_classifiers (
        ArtificerArtifact_id bigint not null,
        classifiers varchar(255)
    );

    create table ArtificerArtifact_normalizedClassifiers (
        ArtificerArtifact_id bigint not null,
        normalizedClassifiers varchar(255)
    );

    create table ArtificerAuditEntry (
        id bigint generated by default as identity,
        lastActionTime timestamp,
        username varchar(50),
        type varchar(20),
        uuid char(36),
        artifact_id bigint,
        primary key (id)
    );

    create table ArtificerAuditItem (
        id bigint generated by default as identity,
        type varchar(20),
        auditEntry_id bigint,
        primary key (id)
    );

    create table ArtificerAuditItem_properties (
        ArtificerAuditItem_id bigint not null,
        properties varchar(255),
        properties_KEY varchar(255),
        primary key (ArtificerAuditItem_id, properties_KEY)
    );

    create table ArtificerComment (
        id bigint generated by default as identity,
        lastActionTime timestamp,
        username varchar(50),
        text clob,
        artifact_id bigint not null,
        primary key (id)
    );

    create table ArtificerDocumentArtifact (
        id bigint not null,
        primary key (id)
    );

    create table ArtificerOntology (
        surrogateId bigint generated by default as identity,
        base varchar(255),
        comment clob,
        createdBy varchar(255),
        createdOn date,
        id varchar(255),
        label varchar(255),
        lastModifiedBy varchar(255),
        lastModifiedOn date,
        uuid char(36),
        primary key (surrogateId)
    );

    create table ArtificerOntologyClass (
        surrogateId bigint generated by default as identity,
        comment clob,
        id varchar(255),
        label varchar(255),
        uri varchar(255),
        parent_surrogateId bigint,
        root_surrogateId bigint,
        primary key (surrogateId)
    );

    create table ArtificerProperty (
        id bigint generated by default as identity,
        custom boolean not null,
        propertyKey varchar(255),
        propertyValue varchar(255),
        owner_id bigint not null,
        primary key (id)
    );

    create table ArtificerRelationship (
        id bigint generated by default as identity,
        name varchar(255),
        type integer,
        owner_id bigint not null,
        primary key (id)
    );

    create table ArtificerRelationship_otherAttributes (
        ArtificerRelationship_id bigint not null,
        otherAttributes varchar(255),
        otherAttributes_KEY varchar(255),
        primary key (ArtificerRelationship_id, otherAttributes_KEY)
    );

    create table ArtificerStoredQuery (
        queryName varchar(255) not null,
        queryExpression varchar(255),
        primary key (queryName)
    );

    create table ArtificerStoredQuery_propertyNames (
        ArtificerStoredQuery_queryName varchar(255) not null,
        propertyNames varchar(255)
    );

    create table ArtificerTarget (
        id bigint generated by default as identity,
        targetType varchar(255),
        relationship_id bigint not null,
        target_id bigint not null,
        primary key (id)
    );

    create table ArtificerTarget_otherAttributes (
        ArtificerTarget_id bigint not null,
        otherAttributes varchar(255),
        otherAttributes_KEY varchar(255),
        primary key (ArtificerTarget_id, otherAttributes_KEY)
    );

    create table ArtificerWsdlDerivedArtifact (
        id bigint not null,
        primary key (id)
    );

    create table ArtificerWsdlDerivedArtifact_ArtificerWsdlDerivedArtifact (
        ArtificerWsdlDerivedArtifact_id bigint not null,
        extension_id bigint not null
    );

    create table ArtificerWsdlDocumentArtifact (
        id bigint not null,
        primary key (id)
    );

    create table ArtificerWsdlDocumentArtifact_ArtificerArtifact (
        ArtificerWsdlDocumentArtifact_id bigint not null,
        simpleTypes_id bigint not null,
        elements_id bigint not null,
        complexTypes_id bigint not null,
        attributes_id bigint not null
    );

    create table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact (
        ArtificerWsdlDocumentArtifact_id bigint not null,
        services_id bigint not null,
        ports_id bigint not null,
        portTypes_id bigint not null,
        parts_id bigint not null,
        operations_id bigint not null,
        operationOutputs_id bigint not null,
        operationInputs_id bigint not null,
        messages_id bigint not null,
        faults_id bigint not null,
        extensions_id bigint not null,
        bindings_id bigint not null,
        bindingOperations_id bigint not null,
        bindingOperationOutputs_id bigint not null,
        bindingOperationInputs_id bigint not null,
        bindingOperationFaults_id bigint not null
    );

    create table ArtificerXsdDocumentArtifact (
        id bigint not null,
        primary key (id)
    );

    create table ArtificerXsdDocumentArtifact_ArtificerArtifact (
        ArtificerXsdDocumentArtifact_id bigint not null,
        simpleTypes_id bigint not null,
        elements_id bigint not null,
        complexTypes_id bigint not null,
        attributes_id bigint not null
    );

    create index artifact_uuid_idx on ArtificerArtifact (uuid);

    create index artifact_name_idx on ArtificerArtifact (name);

    create index artifact_model_idx on ArtificerArtifact (model);

    create index artifact_type_idx on ArtificerArtifact (type);

    create index artifact_model_type_idx on ArtificerArtifact (model, type);

    create index relationship_name_idx on ArtificerRelationship (name);

    create index storedquery_name_idx on ArtificerStoredQuery (queryName);

    alter table ArtificerWsdlDerivedArtifact_ArtificerWsdlDerivedArtifact 
        add constraint UK_mqmkpiqop0cu4jdco09lq9ovq unique (extension_id);

    alter table ArtificerWsdlDocumentArtifact_ArtificerArtifact 
        add constraint UK_otvk766m3of6vkmrln2bwyxhj unique (simpleTypes_id);

    alter table ArtificerWsdlDocumentArtifact_ArtificerArtifact 
        add constraint UK_j0s1hqp47o0qowi8aiypitjvx unique (elements_id);

    alter table ArtificerWsdlDocumentArtifact_ArtificerArtifact 
        add constraint UK_3nj0ti0qptltfho2kgcwm3hj8 unique (complexTypes_id);

    alter table ArtificerWsdlDocumentArtifact_ArtificerArtifact 
        add constraint UK_f1os5u82f30vgx8lvd6dxmh01 unique (attributes_id);

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint UK_gb9laes1rvv4h2rqancigwbo3 unique (services_id);

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint UK_iwivhbovmjnseyl6e38lj60m2 unique (ports_id);

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint UK_s39943fx4ucrwfbt2a8br8v7q unique (portTypes_id);

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint UK_skalp1atlfqhpj36625ma739n unique (parts_id);

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint UK_bp9ulkv5w4xi6lhaw6kcpxx92 unique (operations_id);

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint UK_1nqvlk1otqwarv0l1ru7hf80r unique (operationOutputs_id);

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint UK_a4f075qkbhbeem67s9o1p1l0q unique (operationInputs_id);

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint UK_5dl84bk7msvd9n2w60cc9gk6n unique (messages_id);

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint UK_ougtts5r2yem3rlplxmdr6hyj unique (faults_id);

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint UK_he15kdi5khs4lck8bovniydea unique (extensions_id);

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint UK_msx8vttrwwdpa3c4v486vwsl0 unique (bindings_id);

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint UK_gp01udjl8tbhsb5f636acmw5f unique (bindingOperations_id);

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint UK_cyifrdhcs7ppe7ylft62frh6m unique (bindingOperationOutputs_id);

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint UK_sjlceeo60gc0w0ukl10fv34nx unique (bindingOperationInputs_id);

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint UK_1lgh2xc7nurmyfqcsj45c7657 unique (bindingOperationFaults_id);

    alter table ArtificerXsdDocumentArtifact_ArtificerArtifact 
        add constraint UK_g1fwnuynjvfvdc0bhqtc7lsnf unique (simpleTypes_id);

    alter table ArtificerXsdDocumentArtifact_ArtificerArtifact 
        add constraint UK_h3a7wjolu3yb7pnn85g7bgjc9 unique (elements_id);

    alter table ArtificerXsdDocumentArtifact_ArtificerArtifact 
        add constraint UK_mrch35w1c4e3av7goxhgoje1w unique (complexTypes_id);

    alter table ArtificerXsdDocumentArtifact_ArtificerArtifact 
        add constraint UK_4hw0cjxhdn9b8t9ikr4ljrfea unique (attributes_id);

    alter table ArtificerArtifact 
        add constraint FK_5he8qx4p9didgdqewhvv0h65e 
        foreign key (derivedFrom_id) 
        references ArtificerArtifact;

    alter table ArtificerArtifact_classifiers 
        add constraint FK_3ksvuyo0tlsaj7r4jkbljs0fs 
        foreign key (ArtificerArtifact_id) 
        references ArtificerArtifact;

    alter table ArtificerArtifact_normalizedClassifiers 
        add constraint FK_n2luspbu4i64tl1wjeirtp6kc 
        foreign key (ArtificerArtifact_id) 
        references ArtificerArtifact;

    alter table ArtificerAuditEntry 
        add constraint FK_2lnlstjxd9rqmv67whu6u8mfy 
        foreign key (artifact_id) 
        references ArtificerArtifact;

    alter table ArtificerAuditItem 
        add constraint FK_20a0yevjg1j36b5m33chpx71q 
        foreign key (auditEntry_id) 
        references ArtificerAuditEntry;

    alter table ArtificerAuditItem_properties 
        add constraint FK_al7mxsh91w8jvkv2wsp00q84q 
        foreign key (ArtificerAuditItem_id) 
        references ArtificerAuditItem;

    alter table ArtificerComment 
        add constraint FK_lk7u6h2hivm5v2qx4q4iomjag 
        foreign key (artifact_id) 
        references ArtificerArtifact;

    alter table ArtificerDocumentArtifact 
        add constraint FK_e4ya5hv4c8gwqda1j9imen8cu 
        foreign key (id) 
        references ArtificerArtifact;

    alter table ArtificerOntologyClass 
        add constraint FK_f0jxuyajwpmer7x36eofvsbco 
        foreign key (parent_surrogateId) 
        references ArtificerOntologyClass;

    alter table ArtificerOntologyClass 
        add constraint FK_20x153qbwgv4ujef0uv1yx962 
        foreign key (root_surrogateId) 
        references ArtificerOntology;

    alter table ArtificerProperty 
        add constraint FK_9tdtxjyo0sbh14w7pf4lgcjt6 
        foreign key (owner_id) 
        references ArtificerArtifact;

    alter table ArtificerRelationship 
        add constraint FK_qgnwqh1inlf26nrlxvq0b1b93 
        foreign key (owner_id) 
        references ArtificerArtifact;

    alter table ArtificerRelationship_otherAttributes 
        add constraint FK_kgsapdnh8i7orp16hcwk6sng8 
        foreign key (ArtificerRelationship_id) 
        references ArtificerRelationship;

    alter table ArtificerStoredQuery_propertyNames 
        add constraint FK_1b4vulcxxvdo68jm5ro88wmy7 
        foreign key (ArtificerStoredQuery_queryName) 
        references ArtificerStoredQuery;

    alter table ArtificerTarget 
        add constraint FK_ai25r336gchbabwphthei037 
        foreign key (relationship_id) 
        references ArtificerRelationship;

    alter table ArtificerTarget 
        add constraint FK_i2hcu61dc77wjeurhmljwseyl 
        foreign key (target_id) 
        references ArtificerArtifact;

    alter table ArtificerTarget_otherAttributes 
        add constraint FK_jjduupt3sotpqdbwlkgeetkme 
        foreign key (ArtificerTarget_id) 
        references ArtificerTarget;

    alter table ArtificerWsdlDerivedArtifact 
        add constraint FK_cjr4q2lqra7ajmmemtuv0qwhj 
        foreign key (id) 
        references ArtificerArtifact;

    alter table ArtificerWsdlDerivedArtifact_ArtificerWsdlDerivedArtifact 
        add constraint FK_mqmkpiqop0cu4jdco09lq9ovq 
        foreign key (extension_id) 
        references ArtificerWsdlDerivedArtifact;

    alter table ArtificerWsdlDerivedArtifact_ArtificerWsdlDerivedArtifact 
        add constraint FK_iinooh16ic9iddggfq8qp6ibi 
        foreign key (ArtificerWsdlDerivedArtifact_id) 
        references ArtificerWsdlDerivedArtifact;

    alter table ArtificerWsdlDocumentArtifact 
        add constraint FK_olb8dn71bkv0003id8172sm2n 
        foreign key (id) 
        references ArtificerDocumentArtifact;

    alter table ArtificerWsdlDocumentArtifact_ArtificerArtifact 
        add constraint FK_otvk766m3of6vkmrln2bwyxhj 
        foreign key (simpleTypes_id) 
        references ArtificerArtifact;

    alter table ArtificerWsdlDocumentArtifact_ArtificerArtifact 
        add constraint FK_kud7h93gwnv1tb8ccef0kpyle 
        foreign key (ArtificerWsdlDocumentArtifact_id) 
        references ArtificerWsdlDocumentArtifact;

    alter table ArtificerWsdlDocumentArtifact_ArtificerArtifact 
        add constraint FK_j0s1hqp47o0qowi8aiypitjvx 
        foreign key (elements_id) 
        references ArtificerArtifact;

    alter table ArtificerWsdlDocumentArtifact_ArtificerArtifact 
        add constraint FK_3nj0ti0qptltfho2kgcwm3hj8 
        foreign key (complexTypes_id) 
        references ArtificerArtifact;

    alter table ArtificerWsdlDocumentArtifact_ArtificerArtifact 
        add constraint FK_f1os5u82f30vgx8lvd6dxmh01 
        foreign key (attributes_id) 
        references ArtificerArtifact;

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint FK_gb9laes1rvv4h2rqancigwbo3 
        foreign key (services_id) 
        references ArtificerWsdlDerivedArtifact;

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint FK_ghhcidjvqgivgb2k1usqg620u 
        foreign key (ArtificerWsdlDocumentArtifact_id) 
        references ArtificerWsdlDocumentArtifact;

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint FK_iwivhbovmjnseyl6e38lj60m2 
        foreign key (ports_id) 
        references ArtificerWsdlDerivedArtifact;

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint FK_s39943fx4ucrwfbt2a8br8v7q 
        foreign key (portTypes_id) 
        references ArtificerWsdlDerivedArtifact;

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint FK_skalp1atlfqhpj36625ma739n 
        foreign key (parts_id) 
        references ArtificerWsdlDerivedArtifact;

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint FK_bp9ulkv5w4xi6lhaw6kcpxx92 
        foreign key (operations_id) 
        references ArtificerWsdlDerivedArtifact;

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint FK_1nqvlk1otqwarv0l1ru7hf80r 
        foreign key (operationOutputs_id) 
        references ArtificerWsdlDerivedArtifact;

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint FK_a4f075qkbhbeem67s9o1p1l0q 
        foreign key (operationInputs_id) 
        references ArtificerWsdlDerivedArtifact;

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint FK_5dl84bk7msvd9n2w60cc9gk6n 
        foreign key (messages_id) 
        references ArtificerWsdlDerivedArtifact;

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint FK_ougtts5r2yem3rlplxmdr6hyj 
        foreign key (faults_id) 
        references ArtificerWsdlDerivedArtifact;

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint FK_he15kdi5khs4lck8bovniydea 
        foreign key (extensions_id) 
        references ArtificerWsdlDerivedArtifact;

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint FK_msx8vttrwwdpa3c4v486vwsl0 
        foreign key (bindings_id) 
        references ArtificerWsdlDerivedArtifact;

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint FK_gp01udjl8tbhsb5f636acmw5f 
        foreign key (bindingOperations_id) 
        references ArtificerWsdlDerivedArtifact;

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint FK_cyifrdhcs7ppe7ylft62frh6m 
        foreign key (bindingOperationOutputs_id) 
        references ArtificerWsdlDerivedArtifact;

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint FK_sjlceeo60gc0w0ukl10fv34nx 
        foreign key (bindingOperationInputs_id) 
        references ArtificerWsdlDerivedArtifact;

    alter table ArtificerWsdlDocumentArtifact_ArtificerWsdlDerivedArtifact 
        add constraint FK_1lgh2xc7nurmyfqcsj45c7657 
        foreign key (bindingOperationFaults_id) 
        references ArtificerWsdlDerivedArtifact;

    alter table ArtificerXsdDocumentArtifact 
        add constraint FK_plul74oxfdh4j7hi34hwferow 
        foreign key (id) 
        references ArtificerDocumentArtifact;

    alter table ArtificerXsdDocumentArtifact_ArtificerArtifact 
        add constraint FK_g1fwnuynjvfvdc0bhqtc7lsnf 
        foreign key (simpleTypes_id) 
        references ArtificerArtifact;

    alter table ArtificerXsdDocumentArtifact_ArtificerArtifact 
        add constraint FK_q7lklcfoq767xuoxbby7x8be7 
        foreign key (ArtificerXsdDocumentArtifact_id) 
        references ArtificerXsdDocumentArtifact;

    alter table ArtificerXsdDocumentArtifact_ArtificerArtifact 
        add constraint FK_h3a7wjolu3yb7pnn85g7bgjc9 
        foreign key (elements_id) 
        references ArtificerArtifact;

    alter table ArtificerXsdDocumentArtifact_ArtificerArtifact 
        add constraint FK_mrch35w1c4e3av7goxhgoje1w 
        foreign key (complexTypes_id) 
        references ArtificerArtifact;

    alter table ArtificerXsdDocumentArtifact_ArtificerArtifact 
        add constraint FK_4hw0cjxhdn9b8t9ikr4ljrfea 
        foreign key (attributes_id) 
        references ArtificerArtifact;
