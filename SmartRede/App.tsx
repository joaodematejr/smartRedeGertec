import React from 'react';
import {
  StyleSheet,
  ScrollView,
  View,
  TouchableOpacity,
  Text,
  NativeModules,
  Image,
} from 'react-native';

import RNFS from 'react-native-fs';
import imgBase64 from './Base64';

function App() {
  const {Rede} = NativeModules;
  const {DEBITO} = Rede;

  function showMsg() {
    Rede.show('teste' as String);
    console.log('teste', DEBITO);
  }

  function handlePayment() {
    Rede.payment(DEBITO as String, (120 * 100) as Number, 1 as Number)
      .then((response: any) => {
        console.log('22', response);
      })
      .catch((error: any) => {
        console.log('25', error);
      });
  }

  function handleReversal() {
    Rede.reversal()
      .then((response: any) => {
        console.log('22', response);
      })
      .catch((error: any) => {
        console.log('25', error);
      });
  }

  function handleReprint() {
    Rede.reprint()
      .then((response: any) => {
        console.log('22', response);
      })
      .catch((error: any) => {
        console.log('25', error);
      });
  }

  function generateImage() {
    let name = new Date().getTime();
    let count = 1;
    const imagePath = `${RNFS.DownloadDirectoryPath}/${name}${count}.jpg`;
    RNFS.writeFile(imagePath, imgBase64, 'base64').then(async function () {});
  }

  function handlePrint() {
    Rede.print()
      .then((response: any) => {
        console.log('22', response);
      })
      .catch((error: any) => {
        console.log('25', error);
      });
  }

  function handleStatusPrint() {
    Rede.getStatusPrint()
      .then((response: any) => {
        console.log('22', response);
      })
      .catch((error: any) => {
        console.log('25', error);
      });
  }

  return (
    <ScrollView style={styles.scrollView}>
      <View style={styles.container}>
        <TouchableOpacity style={styles.button} onPress={() => showMsg()}>
          <Text>Mostrar Mensagem</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.button} onPress={() => handlePayment()}>
          <Text>Pagamento</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.button}
          onPress={() => handleReversal()}>
          <Text>Reembolso</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.button} onPress={() => handleReprint()}>
          <Text>Reimpressão</Text>
        </TouchableOpacity>

        <Image
          style={{width: 500, height: 500, resizeMode: 'contain'}}
          source={{
            uri: `data:image/jpg;base64,${imgBase64}`,
          }}
        />

        <TouchableOpacity style={styles.button} onPress={() => generateImage()}>
          <Text>Gerar Imagem</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.button} onPress={() => handleStatusPrint()}>
          <Text>Status Impressora</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.button} onPress={() => handlePrint()}>
          <Text>Imprimir Imagem</Text>
        </TouchableOpacity>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  button: {
    alignItems: 'center',
    backgroundColor: '#DDDDDD',
    padding: 15,
  },
  scrollView: {
    marginHorizontal: 20,
  },
});

export default App;
