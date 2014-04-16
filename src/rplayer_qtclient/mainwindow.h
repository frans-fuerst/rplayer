#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include "ui_mainwindow.h"

#include <QtGui/QMainWindow>
#include <QtGui/QListWidget>
#include <QtCore/QTimer>
#include <QtNetwork/QUdpSocket>
#include <QtNetwork/QTcpSocket>

#include <stdarg.h>
#if !defined(Q_WS_S60)
#include <stdio.h>
#endif


/// TODO:
/// send 'reload'
/// v reconnect

class MainWindow : public QMainWindow
{
    Q_OBJECT

public:

    explicit MainWindow(QWidget *parent = 0)
        : QMainWindow(parent)
        , ui(new Ui::MainWindow)
        , m_rPlayerServerPort( 2010 )
        , m_rPlayerBroadcastPort( 2011 )
        , m_rPlayerTcpPort( 2012 )
        , m_rPlayerServerHost( QHostAddress("192.168.1.1") )
        , m_rPlayerServerUID( 0 )
        , m_nextBlockSize( 0 )
    {
        ui->setupUi(this);

        m_udpReceiverSocket = new QUdpSocket(this);
        m_udpSenderSocket = new QUdpSocket(this);
        m_tcpSocket = new QTcpSocket(this);

        m_networkMan = new QTimer();

        connect( ui->m_pbBan,        SIGNAL( clicked()),  this, SLOT(pbBan()) );
        connect( ui->m_pbLove,       SIGNAL( clicked()),  this, SLOT(pbLove()) );
        connect( ui->m_pbQuitPlayer, SIGNAL( clicked()),  this, SLOT(pbQuit()) );
        connect( ui->m_pbVolDown,    SIGNAL( clicked()),  this, SLOT(pbVolDownClicked()) );
        connect( ui->m_pbVolUp,      SIGNAL( clicked()),  this, SLOT(pbVolUpClicked()) );
        connect( ui->m_pbSend,       SIGNAL( clicked()),  this, SLOT(pbSendClicked()) );

        connect( ui->m_pbPlayPause,  SIGNAL( clicked()),  this, SLOT(pbPlayPauseClicked() ) );
        connect( ui->m_pbSkip,       SIGNAL( clicked()),  this, SLOT(pbSkipClicked()) );

        connect( m_udpReceiverSocket, SIGNAL( readyRead()),                         this, SLOT( slotReadPendingDatagrams()) );
        connect( this,                SIGNAL( signalHandleUDPData(QByteArray)),     this, SLOT( slotHandleUDPData(QByteArray)) );

        connect( m_tcpSocket,         SIGNAL( readyRead()),                         this, SLOT( slotTcpReadyRead()));
        connect( m_tcpSocket,         SIGNAL( error(QAbstractSocket::SocketError)), this, SLOT( slotTcpSocketError(QAbstractSocket::SocketError) ));
        connect( m_tcpSocket,         SIGNAL( connected()), this, SLOT( slotTcpConnected() ));
        connect( m_tcpSocket,         SIGNAL( disconnected()), this, SLOT( slotTcpDisconnected() ));
        connect( m_tcpSocket,         SIGNAL( stateChanged(QAbstractSocket::SocketState)), this, SLOT( slotTcpStateChanged(QAbstractSocket::SocketState) ));

        connect( m_networkMan,        SIGNAL( timeout() ), this, SLOT( slotEstablishTCPConnection()) );


        m_udpReceiverSocket->bind( m_rPlayerBroadcastPort, QUdpSocket::ShareAddress);

        /// send a dummy datagram to initialize networking
        m_udpSenderSocket->writeDatagram( "", m_rPlayerServerHost, m_rPlayerServerPort );

        m_networkMan->setInterval( 5000 );
    }

    void handleUDPData( const QByteArray &data )
    {
        emit signalHandleUDPData( data );
    }

    virtual ~MainWindow()
    {
        delete ui;
    }

signals:

    void signalHandleUDPData( const QByteArray & );

private slots:

    void slotEstablishTCPConnection()
    {
        if( m_tcpSocket->state() == QAbstractSocket::UnconnectedState )
        {
            tracemessage( "slotEstablishTCPConnection: connect to %s.. \n", m_rPlayerServerHost.toString().toStdString().c_str() );
            m_tcpSocket->abort();
            m_tcpSocket->connectToHost( m_rPlayerServerHost, m_rPlayerTcpPort );
        }
    }

    void slotTcpReadyRead()
    {
        tracemessage(
               "\n"
               "slotTcpReadyRead(%lld)\n", m_tcpSocket->bytesAvailable() );

        while( m_tcpSocket->canReadLine() )
        {
            QString l_line( m_tcpSocket->readLine() );

            /// remove trailing new line
            l_line = l_line.mid( 0, l_line.length() - 1 );


            if( l_line.indexOf( " " ) == -1 ) return;

            QString l_opcode = l_line.mid( 0, l_line.indexOf( " " ) );
            QString l_param = l_line.mid( l_line.indexOf( " " ) + 1 );

            if      ( l_opcode == "playing" ) handleNewItem( l_param );
            else if ( l_opcode == "volume" )  handleVolume( l_param );
            else tracemessage( "slotTcpReadyRead(): received unknown block: '%s' (%d)\n", l_line.toStdString().c_str(), l_line.length() );
        }
    }

    void slotTcpConnected()
    {
        tracemessage("slotTcpConnected\n");
        ui->m_lblStatus->setText("|");

//        sendMessage( "basedir C:/3SOFT/_200805_Privat" );
//        sendMessage( "reload" );
    }

    void slotTcpDisconnected()
    {
        tracemessage("slotTcpDisconnected\n");
        ui->m_lblStatus->setText("--");
//                slotEstablishTCPConnection();
    }

    void slotTcpStateChanged( QAbstractSocket::SocketState e )
    {
        switch ( e )
        {
            case QAbstractSocket::UnconnectedState: tracemessage("slotTcpStateChanged 'UnconnectedState'\n"); break;
            case QAbstractSocket::HostLookupState:  tracemessage("slotTcpStateChanged 'HostLookupState'\n");  break;
            case QAbstractSocket::ConnectingState:  tracemessage("slotTcpStateChanged 'ConnectingState'\n");  break;
            case QAbstractSocket::ConnectedState:   tracemessage("slotTcpStateChanged 'ConnectedState'\n");   break;
            case QAbstractSocket::BoundState:       tracemessage("slotTcpStateChanged 'BoundState'\n");       break;
            case QAbstractSocket::ListeningState:   tracemessage("slotTcpStateChanged 'ListeningState'\n");   break;
            case QAbstractSocket::ClosingState:     tracemessage("slotTcpStateChanged 'ClosingState'\n");     break;
        }
    }

    void slotTcpSocketError( const QAbstractSocket::SocketError &e )
    {
        tracemessage("slotTcpSocketError: %d\n", e );
        /*
        ConnectionRefusedError,
        RemoteHostClosedError,
        HostNotFoundError,
        SocketAccessError,
        SocketResourceError,
        SocketTimeoutError,                     // 5
        DatagramTooLargeError,
        NetworkError,
        AddressInUseError,
        SocketAddressNotAvailableError,
        UnsupportedSocketOperationError,        // 10
        UnfinishedSocketOperationError,
        ProxyAuthenticationRequiredError,
        SslHandshakeFailedError,
        ProxyConnectionRefusedError,
        ProxyConnectionClosedError,             // 15
        ProxyConnectionTimeoutError,
        ProxyNotFoundError,
        ProxyProtocolError,

        UnknownSocketError = -1
        */
    }

    void pbPlayPauseClicked( void )
    {
        sendMessage( "playpause" );
    }

    void pbSkipClicked()
    {
        sendMessage( "skip" );
    }

    void pbSendClicked()
    {
        sendMessage( ui->m_text->text() );
    }

    void pbBan()
    {
        sendMessage( "ban " + concatenateSelectedTrackElement() );
        sendMessage( "skip" );
    }

    void pbLove()
    {
        sendMessage( "love " + concatenateSelectedTrackElement() );
    }

    void pbQuit()
    {
        sendMessage( "quit" );
    }

    void pbVolDownClicked()
    {
        sendMessage( "voldown" );
    }

    void pbVolUpClicked()
    {
        sendMessage( "volup" );
    }

    void slotHandleUDPData( const QByteArray &a_data )
    {

        QString l_display = m_rPlayerServerHost.toString() + ": '" + QString(a_data) + "'";

        tracemessage( l_display.toStdString().c_str() );
    }

    void slotReadPendingDatagrams()
    {
        while( m_udpReceiverSocket->hasPendingDatagrams() )
        {
            QByteArray l_datagram;
            l_datagram.resize( m_udpReceiverSocket->pendingDatagramSize() );
            QHostAddress sender;
            quint16 senderPort;

            m_udpReceiverSocket->readDatagram( l_datagram.data(), l_datagram.size(), &sender, &senderPort );

            QString l_data = QString( l_datagram.data() );
            if( l_data.indexOf( " " ) == -1 ) break;

            QString l_opcode = l_data.mid( 0, l_data.indexOf( " " ) );
            QString l_param = l_data.mid( l_data.indexOf( " " ) + 1 );

//                tracemessage( "'%s' '%s' '%s'\n", l_data.toStdString().c_str(), l_opcode.toStdString().c_str(), l_param.toStdString().c_str() );

            if( l_opcode.startsWith("rplayer") )
            {
                /// we know it already
                if( m_rPlayerServerHost == sender )
                {
                    if( m_tcpSocket->state() != QAbstractSocket::UnconnectedState ) break;
                }

                /// read the server session id to identify the same server among
                /// different interfaces
                int l_rPlayerServerUID = l_param.toInt();

                /// we know it already
                if( l_rPlayerServerUID == m_rPlayerServerUID ) break;

                /// otherwise save the servers UID
                m_rPlayerServerUID = l_rPlayerServerUID;

                /// and it's IP address
                m_rPlayerServerHost = sender;

                /// display servers IP address
                ui->m_lblIP->setText(m_rPlayerServerHost.toString());

                tracemessage( "slotReadPendingDatagrams: register new rServer at '%s'\n", m_rPlayerServerHost.toString().toStdString().c_str() );

                slotEstablishTCPConnection();
                m_networkMan->setInterval( 5000 );
                m_networkMan->start();
            }
            else
            {
                slotHandleUDPData( l_datagram );
            }
        }
    }

private:

    QString concatenateSelectedTrackElement()
    {
        QString l_element;
        for( int i = ui->m_lstTrack->count(); i > 0; --i)
        {
            if( ui->m_lstTrack->item( i - 1 )->text().trimmed() != "."
             && ui->m_lstTrack->item( i - 1 )->text().trimmed() != "" )
            {
                if( l_element != "" ) l_element += "/";
                l_element += ui->m_lstTrack->item( i - 1 )->text();
                if( ui->m_lstTrack->isItemSelected( ui->m_lstTrack->item( i - 1 ) )) break;
            }
        }
        return l_element;
    }

    void handleNewItem( const QString &l_param )
    {
        QString l_theme = l_param.mid( 0, l_param.indexOf( " " ) );
        QString l_title = l_param.mid( l_param.indexOf( " " ) + 1 );

        tracemessage("now playing theme %s, track '%s'\n",l_theme.toStdString().c_str(), l_title.toStdString().c_str() );
//        ui->m_lblTrack->setText( l_title );
        setWindowTitle( QString("rPlayer-Controller - ") + l_theme );

        QStringList l_elements( l_title.split( "/" ) );

        ui->m_lstTrack->clear();
        QListIterator< QString > i(l_elements);
        i.toBack();
        while (i.hasPrevious())
        {
            ui->m_lstTrack->addItem( i.previous() );
        }
    }

    void handleVolume( const QString &l_param )
    {
        float l_volume = atof( l_param.toStdString().c_str() );
        tracemessage("volume is now %d\n", int( l_volume * 100 ) );
    }

    void sendMessage( const QString &a_msg )
    {
        tracemessage( "sending '%s'\n", a_msg.toStdString().c_str() );
        if( m_tcpSocket->state() == QAbstractSocket::ConnectedState )
        {
            m_tcpSocket->write( (a_msg + "\n").toAscii() );
        }
        else
        {
            m_udpSenderSocket->writeDatagram( a_msg.toAscii(), m_rPlayerServerHost, m_rPlayerServerPort );
        }
    }

    void tracemessage( const char * a_format, ... )
    {
        char l_buffer[1024];
        va_list l_args;
        va_start (l_args, a_format);
        vsprintf ( l_buffer, a_format, l_args );
        va_end( l_args );
#if !defined(Q_WS_S60)
        printf( "%s", l_buffer );
#endif
        ui->m_lblText->setText(l_buffer );
    }


private:

    Ui::MainWindow *ui;

    QUdpSocket *m_udpReceiverSocket;

    QUdpSocket *m_udpSenderSocket;

    QTcpSocket *m_tcpSocket;

    QTimer *m_networkMan;

    int m_rPlayerServerPort;

    int m_rPlayerBroadcastPort;

    int m_rPlayerTcpPort;

    int m_rPlayerServerUID;

    unsigned int m_nextBlockSize;

    QHostAddress m_rPlayerServerHost;
};

#endif // MAINWINDOW_H
