import React from 'react';
import {
  StyleSheet,
  ScrollView,
  View,
  TouchableOpacity,
  Text,
  NativeModules,
} from 'react-native';

function App() {
  const {Rede} = NativeModules;
  const {CREDITO_PARCELADO} = Rede;

  function showMsg() {
    Rede.show('teste' as String);
    console.log('teste', CREDITO_PARCELADO);
  }

  function handlePayment() {
    Rede.payment(
      CREDITO_PARCELADO as String,
      (120 * 100) as Number,
      2 as Number,
    )
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
